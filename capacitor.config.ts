import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.smartstudy.app',
  appName: 'Smart Study',
  webDir: 'dist',
  android: {
    allowMixedContent: true,
  },
};

export default config;
