import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Bootstrap the standalone root component with the app configuration,
// logging any startup failure to the console.
bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
