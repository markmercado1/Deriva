import test from 'node:test';
import assert from 'node:assert/strict';
import {classifyTab} from '../chromium/report.js';

const window = {id: 1, focused: true, incognito: false, type: 'normal'};
const tab = {windowId: 1, active: true, incognito: false, url: 'https://www.youtube.com/watch?v=secret'};
test('only host is extracted, not paths or queries', () => {
  assert.deepEqual(classifyTab(window, tab), {state: 'NORMAL', host: 'youtube.com'});
});
test('private window and tab are excluded before reading URL', () => {
  const forbidden = {windowId: 1, active: true, incognito: true, get url() { throw Error('must not read URL'); }};
  assert.deepEqual(classifyTab(window, forbidden), {state:'UNKNOWN',host:null});
  assert.deepEqual(classifyTab({...window,incognito:true}, tab), {state:'UNKNOWN',host:null});
});
test('blur, inactive tabs, wrong windows and missing data are unknown', () => {
  for (const [w,t] of [[{...window,focused:false},tab],[window,{...tab,active:false}],
    [window,{...tab,windowId:2}],[null,tab],[window,null]]) {
    assert.deepEqual(classifyTab(w,t), {state:'UNKNOWN',host:null});
  }
});
test('internal pages never report their URL as host', () => {
  for (const url of ['chrome://extensions','file:///private.txt','about:blank']) {
    assert.deepEqual(classifyTab(window,{...tab,url}), {state:'INTERNAL',host:null});
  }
});
test('subdomains stay distinct and invalid URLs are excluded', () => {
  assert.equal(classifyTab(window,{...tab,url:'https://docs.google.com/document'}).host,'docs.google.com');
  assert.equal(classifyTab(window,{...tab,url:'https://mail.google.com/'}).host,'mail.google.com');
  assert.deepEqual(classifyTab(window,{...tab,url:'not a url'}),{state:'UNKNOWN',host:null});
});
