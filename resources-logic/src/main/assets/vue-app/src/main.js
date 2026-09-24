// SPDX-License-Identifier: EUPL-1.2

import { mobileApp } from '@edim/mobile-ui';
import '@edim/mobile-ui/dist/style.css';

window.lxBridge = {
  postMessage: (bridgeName, payload) => {
    if (window[bridgeName] && typeof window[bridgeName].handleRequest === 'function') {
      window[bridgeName].handleRequest(JSON.stringify(payload));
      return;
    }

    console.warn(`No bridge found '${bridgeName}'`, payload);
  },
};

mobileApp.mount('#app');