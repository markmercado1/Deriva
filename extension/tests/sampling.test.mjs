import test from 'node:test';
import assert from 'node:assert/strict';
import {settleObservation} from '../chromium/sampling.js';

const normal = (host) => ({state: 'NORMAL', host, retry: false});
const retry = () => ({state: 'UNKNOWN', host: null, retry: true});
const unknown = () => ({state: 'UNKNOWN', host: null, retry: false});

test('a stable observation is returned without delay', async () => {
  let reads = 0;
  let delays = 0;
  const report = await settleObservation(async () => { reads++; return normal('a.example'); },
      () => 0, async () => { delays++; });
  assert.deepEqual(report, normal('a.example'));
  assert.equal(reads, 1);
  assert.equal(delays, 0);
});

test('a revision change during the read triggers a fresh read, not a false UNKNOWN', async () => {
  let revision = 0;
  const reports = [normal('old.example'), normal('new.example')];
  let reads = 0;
  let delays = 0;
  const report = await settleObservation(async () => {
    const next = reports[reads++];
    if (reads === 1) revision++; // An event arrived while the tab was being read.
    return next;
  }, () => revision, async () => { delays++; });
  assert.deepEqual(report, normal('new.example'));
  assert.equal(reads, 2);
  assert.equal(delays, 1);
});

test('a loading tab without URL is retried and never reuses the previous host', async () => {
  const reports = [retry(), normal('fresh.example')];
  let reads = 0;
  let delays = 0;
  const report = await settleObservation(async () => reports[reads++],
      () => 0, async () => { delays++; });
  assert.deepEqual(report, normal('fresh.example'));
  assert.equal(reads, 2);
  assert.equal(delays, 1);
});

test('a genuine UNKNOWN is returned immediately and is not retried', async () => {
  let reads = 0;
  let delays = 0;
  const report = await settleObservation(async () => { reads++; return unknown(); },
      () => 0, async () => { delays++; });
  assert.deepEqual(report, unknown());
  assert.equal(reads, 1);
  assert.equal(delays, 0);
});

test('a genuine UNKNOWN is not delayed by a revision event during the read', async () => {
  let revision = 0;
  let reads = 0;
  let delays = 0;
  const report = await settleObservation(async () => {
    reads++;
    revision++; // e.g. the focus event that produced the UNKNOWN
    return unknown();
  }, () => revision, async () => { delays++; });
  assert.deepEqual(report, unknown());
  assert.equal(reads, 1);
  assert.equal(delays, 0);
});

test('the retry is bounded and falls back to one last fresh read', async () => {
  let reads = 0;
  let delays = 0;
  const report = await settleObservation(async () => { reads++; return retry(); },
      () => 0, async () => { delays++; }, 2);
  assert.deepEqual(report, retry());
  assert.equal(reads, 3); // two loop reads plus the final fresh read
  assert.equal(delays, 1);
});
