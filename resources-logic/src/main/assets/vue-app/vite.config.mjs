// SPDX-License-Identifier: EUPL-1.2

/* eslint-disable no-console -- for debugging */
/* eslint-disable no-restricted-imports -- vite doesn`t use aliases in imports */
/* eslint-disable import/no-extraneous-dependencies -- vite mostly should use dev dependencies */
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import { createHtmlPlugin } from 'vite-plugin-html';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import mkcert from 'vite-plugin-mkcert';
import { fileURLToPath } from 'url';
import dns from 'dns';
import packageJson from './package.json' assert { type: 'json' };

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// set ip4 as default dns lookup in order to support localhost
// https://vitejs.dev/config/server-options.html#server-host
dns.setDefaultResultOrder('ipv4first');

const CONFIG = {
  appName: 'webapp',
  appDescription: 'Embeddable WebApp',
  defaultUrl: 'https://localhost:44341/',
  defaultClient: 'webapp',
};

const getEnvVariables = (mode, serving) => {
  // Load env file based on `mode` in the current working directory.
  // Set the third parameter to '' to load all env regardless of the `VITE_` prefix.
  const env = loadEnv(mode, process.cwd(), '');
  const envVariables = {
    VUE_APP_VERSION: packageJson.version,
    VUE_APP_ENVIRONMENT: serving ? 'local' : env.NODE_ENV,
    VUE_APP_NAME: CONFIG.appName,
    VUE_APP_DESCRIPTION: CONFIG.appDescription,
    BASE_PATH: env.BASE_PATH,

    // dev only
    VUE_APP_SERVICE_URL_PROXY: '',
  };
  if (serving) {
    envVariables.BASE_URL = env.PUBLIC_URL || CONFIG.defaultUrl;
    envVariables.BASE_PATH = envVariables.BASE_PATH || '';
    envVariables.VUE_APP_VERSION += '-serve';
    envVariables.USE_MOCK_MIDDLEWARE = env.USE_MOCK_MIDDLEWARE === 'true';
  } else {
    envVariables.VUE_APP_ENVIRONMENT = env.NODE_ENV;
    envVariables.BASE_PATH = envVariables.BASE_PATH || '';
  }
  return envVariables;
};

/**
 * @returns { import('vite').UserConfigExport }
 * @description https://vitejs.dev/config/
 */
export default defineConfig((command) => {
  const serving = command?.command === 'serve' && command?.mode === 'development';
  const envVariables = getEnvVariables(command.mode, serving);
  return {
    base: envVariables.BASE_PATH,
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
        '/lx-fonts': fileURLToPath(
          new URL('./node_modules/@wntr/lx-ui/dist/lx-fonts', import.meta.url)
        ),
      },
    },
    plugins: [
      vue(),
      createHtmlPlugin({
        minify: true,
        inject: {
          data: envVariables,
        },
      }),
      viteStaticCopy({
        targets: [
          {
            src: './node_modules/@edim/mobile-ui/dist/imgs/*',
            dest: './assets/',
          },
          {
            src: './node_modules/@edim/mobile-ui/dist/*',
            dest: './assets/',
          }
        ],
      }),
      serving && mkcert(),
    ],
    build: {
      // https://vitejs.dev/config/#build-target
      target: ['es2020'],
      outDir: './dist',
      // Default to OFF for production builds; enable only when explicitly requested.
      sourcemap: process.env.SOURCEMAP === 'true',
    },
    server: {},
    test: {
      globals: true,
      setupFiles: [path.resolve(__dirname, './tests/setup.js')],
      environment: 'jsdom',
      isolate: false,
      include: ['tests/unit/**/*.js'],
    },
  };
});
