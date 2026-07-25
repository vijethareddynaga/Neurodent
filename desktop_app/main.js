const { app, BrowserWindow, Menu, shell } = require('electron');
const path = require('path');

const TARGET_URL = 'https://cozy-semolina-6fc689.netlify.app/';

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 850,
    minWidth: 900,
    minHeight: 600,
    title: "NeuroDent AI - Desktop Application",
    icon: path.join(__dirname, '../neurodental web/oral web/logo.png'),
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true
    },
    autoHideMenuBar: false,
    backgroundColor: '#ffffff'
  });

  // Load Netlify Web App URL
  mainWindow.loadURL(TARGET_URL);

  // Fallback to local index.html if network fails
  mainWindow.webContents.on('did-fail-load', () => {
    console.log("Failed to load remote URL, serving local web app...");
    mainWindow.loadFile(path.join(__dirname, '../neurodental web/oral web/index.html'));
  });

  // Open external links in default OS browser
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (!url.startsWith(TARGET_URL)) {
      shell.openExternal(url);
      return { action: 'deny' };
    }
    return { action: 'allow' };
  });

  // Application Menu
  const menuTemplate = [
    {
      label: 'NeuroDent AI',
      submenu: [
        { label: 'Reload App', accelerator: 'CmdOrCtrl+R', click: () => mainWindow.reload() },
        { label: 'Toggle Full Screen', accelerator: 'F11', click: () => mainWindow.setFullScreen(!mainWindow.isFullScreen()) },
        { type: 'separator' },
        { label: 'Exit', accelerator: 'CmdOrCtrl+Q', click: () => app.quit() }
      ]
    },
    {
      label: 'Navigation',
      submenu: [
        { label: 'Home Page', click: () => mainWindow.loadURL(TARGET_URL) },
        { label: 'Dashboard', click: () => mainWindow.loadURL(TARGET_URL + 'dashboard.html') },
        { label: 'Login', click: () => mainWindow.loadURL(TARGET_URL + 'login.html') },
        { label: 'Register', click: () => mainWindow.loadURL(TARGET_URL + 'register.html') }
      ]
    },
    {
      label: 'Help',
      submenu: [
        { label: 'Open in Web Browser', click: () => shell.openExternal(TARGET_URL) },
        { label: 'GitHub Repository', click: () => shell.openExternal('https://github.com/vijethareddynaga/Neurodent') }
      ]
    }
  ];

  const menu = Menu.buildFromTemplate(menuTemplate);
  Menu.setApplicationMenu(menu);

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (mainWindow === null) {
    createWindow();
  }
});
