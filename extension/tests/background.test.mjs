import test from 'node:test';
import assert from 'node:assert/strict';

test('a tab event during the read re-reads the tab instead of posting UNKNOWN', async () => {
  const listeners = {
    tabsActivated: [], tabsUpdated: [], tabsRemoved: [], windowsFocus: [],
    startup: [], installed: [], alarm: [], storage: [], message: [], action: []
  };
  const event = (name) => ({addListener: (fn) => listeners[name].push(fn)});
  let queries = 0;
  const window = {id: 1, focused: true, incognito: false, type: 'normal'};
  const tab = (url) => ({windowId: 1, active: true, incognito: false, url, status: 'complete'});

  globalThis.chrome = {
    storage: {local: {get: async () => ({port: 47821, token: 'a'.repeat(64)})},
      onChanged: event('storage')},
    windows: {getAll: async () => [window], get: async () => window,
      onFocusChanged: event('windowsFocus')},
    tabs: {
      query: async () => {
        queries++;
        if (queries === 1) listeners.tabsActivated.forEach(fn => fn({tabId: 2}));
        return [tab(queries === 1 ? 'https://old.example/' : 'https://new.example/')];
      },
      onActivated: event('tabsActivated'), onUpdated: event('tabsUpdated'), onRemoved: event('tabsRemoved')
    },
    runtime: {onStartup: event('startup'), onInstalled: event('installed'), onMessage: event('message')},
    alarms: {create: async () => {}, onAlarm: event('alarm')},
    action: {onClicked: event('action')}
  };

  const posted = [];
  let resolveTab;
  const tabPost = new Promise(resolve => { resolveTab = resolve; });
  globalThis.fetch = async (_url, options) => {
    const body = JSON.parse(options.body);
    if (body.browser === 'chrome' && !body.state) {
      return {ok: true, status: 200, json: async () => ({session: 'b'.repeat(36)})};
    }
    posted.push(body);
    resolveTab();
    return {ok: true, status: 204};
  };
  const realSetInterval = globalThis.setInterval;
  globalThis.setInterval = () => 0;
  try {
    await import('../chromium/background.js?race-test');
    let timer;
    const timeout = new Promise((_, reject) => { timer = setTimeout(() => reject(Error('no report')), 3000); });
    try {
      await Promise.race([tabPost, timeout]);
    } finally {
      clearTimeout(timer);
    }
    assert.equal(posted.length, 1);
    assert.equal(posted[0].state, 'NORMAL');
    assert.equal(posted[0].host, 'new.example');
    assert.deepEqual(Object.keys(posted[0]).sort(), ['browser', 'host', 'seq', 'session', 'state']);
  } finally {
    globalThis.setInterval = realSetInterval;
    delete globalThis.chrome;
    delete globalThis.fetch;
  }
});
