export const E2E_TARGET = process.env.E2E_TARGET ?? 'local';

export const USER_BASE_URL = process.env.USER_BASE_URL ?? 'http://127.0.0.1:4173';
export const ADMIN_BASE_URL = process.env.ADMIN_BASE_URL ?? 'http://127.0.0.1:4174';

export const USER_TEST_EMAIL = process.env.USER_TEST_EMAIL ?? 'test1@restart-point.com';
export const USER_TEST_PASSWORD = process.env.USER_TEST_PASSWORD ?? 'test1234';
export const ADMIN_TEST_EMAIL = process.env.ADMIN_TEST_EMAIL ?? 'admin@restart-point.com';
export const ADMIN_TEST_PASSWORD = process.env.ADMIN_TEST_PASSWORD ?? '';

export const isProductionTarget = E2E_TARGET === 'production';
