import os
import io
import hashlib
from datetime import datetime, date

import numpy as np
import onnxruntime as ort
from PIL import Image
from cachetools import TTLCache

from fastapi import FastAPI, File, UploadFile, Depends, HTTPException, status, Form
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from pydantic import BaseModel

from sqlalchemy import create_engine, Column, Integer, String, Float, ForeignKey, DateTime, Date
from sqlalchemy.orm import declarative_base, sessionmaker, Session, relationship
from passlib.context import CryptContext

from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./neurodentai.db")

# Setup SQLAlchemy
if DATABASE_URL.startswith("postgresql"):
    engine = create_engine(DATABASE_URL)
else:
    engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
    
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True)
    full_name = Column(String, nullable=True)
    password_hash = Column(String)
    
    patients = relationship("Patient", back_populates="doctor")

class Patient(Base):
    __tablename__ = "patients"
    id = Column(Integer, primary_key=True, index=True)
    doctor_id = Column(Integer, ForeignKey("users.id"))
    patient_id_display = Column(String) # e.g., Patient #1042
    name = Column(String)
    age = Column(Integer)
    gender = Column(String)
    nerve_damage = Column(Float)
    risk_level = Column(String) # High Risk, Low Risk, Medium Risk
    date_added = Column(Date, default=date.today)
    
    doctor = relationship("User", back_populates="patients")

Base.metadata.create_all(bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

import bcrypt

def verify_password(plain_password, hashed_password):
    return bcrypt.checkpw(plain_password.encode('utf-8'), hashed_password.encode('utf-8'))

def get_password_hash(password):
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

# --- FastAPI App ---
app = FastAPI(title="NeuroDentAI API")

from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- ML Setup ---
prediction_cache = TTLCache(maxsize=100, ttl=3600)
ort_session = None

def get_ort_session():
    global ort_session
    if ort_session is None:
        model_path = "nerve_damage_resnet50.onnx"
        if os.path.exists(model_path):
            ort_session = ort.InferenceSession(model_path, providers=['CPUExecutionProvider'])
        else:
            print("WARNING: ONNX model not found.")
    return ort_session

def preprocess_image(image: Image.Image) -> np.ndarray:
    image = image.resize((224, 224), Image.Resampling.BILINEAR)
    img_data = np.array(image, dtype=np.float32) / 255.0
    mean = np.array([0.485, 0.456, 0.406], dtype=np.float32)
    std = np.array([0.229, 0.224, 0.225], dtype=np.float32)
    img_data = (img_data - mean) / std
    img_data = np.transpose(img_data, (2, 0, 1))
    img_data = np.expand_dims(img_data, axis=0)
    return img_data

# --- Pydantic Models ---
class UserCreate(BaseModel):
    username: str
    password: str
    full_name: str = ""

class PatientCreate(BaseModel):
    doctor_id: int
    name: str
    age: int
    gender: str
    nerve_damage: float

# --- Routes ---

@app.post("/register")
def register(user: UserCreate, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.username == user.username).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Username already registered")
    
    hashed_password = get_password_hash(user.password)
    new_user = User(username=user.username, full_name=user.full_name, password_hash=hashed_password)
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    
    return {"id": new_user.id, "username": new_user.full_name or new_user.username}

@app.post("/login")
def login(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.username == form_data.username).first()
    if not user or not verify_password(form_data.password, user.password_hash):
        raise HTTPException(status_code=400, detail="Incorrect username or password")
    
    return {"id": user.id, "username": user.full_name or user.username}

@app.get("/dashboard/{doctor_id}")
def get_dashboard(doctor_id: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == doctor_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Doctor not found")
        
    total_patients = db.query(Patient).filter(Patient.doctor_id == doctor_id).count()
    todays_patients = db.query(Patient).filter(
        Patient.doctor_id == doctor_id, 
        Patient.date_added == date.today()
    ).count()
    
    recent_patients_query = db.query(Patient).filter(
        Patient.doctor_id == doctor_id
    ).order_by(Patient.id.desc()).limit(10).all()
    
    recent_activity = []
    for p in recent_patients_query:
        recent_activity.append({
            "id": p.id,
            "patient_id_display": p.patient_id_display,
            "name": p.name,
            "age": p.age,
            "gender": p.gender,
            "nerve_damage": p.nerve_damage,
            "risk_level": p.risk_level
        })
        
    return {
        "username": user.full_name or user.username,
        "total_patients": total_patients,
        "todays_patients": todays_patients,
        "recent_activity": recent_activity
    }

@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    contents = await file.read()
    image_hash = hashlib.md5(contents).hexdigest()
    
    if image_hash in prediction_cache:
        return {"percentage": prediction_cache[image_hash], "cached": True}
        
    image = Image.open(io.BytesIO(contents)).convert('RGB')
    
    # --- Image Validation ---
    # Dental X-rays are grayscale. Real photos have high color variance.
    import numpy as np
    img_array = np.array(image, dtype=np.int32)
    r, g, b = img_array[:,:,0], img_array[:,:,1], img_array[:,:,2]
    color_variance = np.mean(np.abs(r - g)) + np.mean(np.abs(g - b)) + np.mean(np.abs(b - r))
    
    if color_variance > 15:
        raise HTTPException(status_code=400, detail="Invalid image: Please upload a valid dental X-ray, not a color photo.")
    # ------------------------

    input_tensor = preprocess_image(image)
    
    session = get_ort_session()
    if session is None:
        raise HTTPException(status_code=500, detail="ONNX prediction model is offline.")
        
    ort_inputs = {session.get_inputs()[0].name: input_tensor}
    ort_outs = session.run(None, ort_inputs)
    percentage = float(ort_outs[0][0][0])
        
    percentage = max(0.0, min(100.0, percentage))
    result = round(percentage, 2)
    prediction_cache[image_hash] = result
    
    return {"percentage": result, "cached": False}

@app.post("/patients")
def add_patient(patient: PatientCreate, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == patient.doctor_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Doctor not found")
        
    risk_level = "Low Risk"
    if patient.nerve_damage > 60:
        risk_level = "High Risk"
    elif patient.nerve_damage > 30:
        risk_level = "Medium Risk"
        
    new_patient = Patient(
        doctor_id=patient.doctor_id,
        name=patient.name,
        age=patient.age,
        gender=patient.gender,
        nerve_damage=patient.nerve_damage,
        risk_level=risk_level
    )
    
    db.add(new_patient)
    db.commit()
    db.refresh(new_patient)
    
    # Generate display ID dynamically based on count of doctor's patients
    patient_count = db.query(Patient).filter(Patient.doctor_id == patient.doctor_id).count()
    new_patient.patient_id_display = f"Patient #{patient_count}"
    db.commit()
    
    return {"id": new_patient.id, "display_id": new_patient.patient_id_display}

@app.get("/patients/{doctor_id}")
def get_patients(doctor_id: int, db: Session = Depends(get_db)):
    patients = db.query(Patient).filter(Patient.doctor_id == doctor_id).order_by(Patient.id.desc()).all()
    return [{
        "id": p.id,
        "patient_id_display": p.patient_id_display,
        "name": p.name,
        "age": p.age,
        "gender": p.gender,
        "nerve_damage": p.nerve_damage,
        "risk_level": p.risk_level
    } for p in patients]

@app.delete("/patients/{patient_id}")
def delete_patient(patient_id: int, db: Session = Depends(get_db)):
    patient = db.query(Patient).filter(Patient.id == patient_id).first()
    if not patient:
        raise HTTPException(status_code=404, detail="Patient not found")
    
    db.delete(patient)
    db.commit()
    return {"message": "Patient deleted successfully"}

class PasswordReset(BaseModel):
    username: str
    new_password: str

@app.post("/reset-password")
def reset_password(data: PasswordReset, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.username == data.username).first()
    if not user:
        raise HTTPException(status_code=404, detail="Email not registered")
    
    user.password_hash = get_password_hash(data.new_password)
    db.commit()
    return {"message": "Password reset successfully"}

@app.delete("/users/{user_id}")
def delete_user(user_id: int, db: Session = Depends(get_db)):
    # Delete associated patients first to prevent constraint issues
    db.query(Patient).filter(Patient.doctor_id == user_id).delete()
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    db.delete(user)
    db.commit()
    return {"message": "Account deleted successfully"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
