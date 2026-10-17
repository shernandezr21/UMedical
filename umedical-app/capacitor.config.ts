import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.umedical.app',
  appName: 'U-Medical',
  webDir: 'www',
  server: {
    cleartext: true, // permite HTTP plano mientras desarrollas (sin esto, Android bloquea http://)
  },
};

export default config;