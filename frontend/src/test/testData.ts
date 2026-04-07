import type { User } from '../types';

export function createUser(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    email: 'test@example.com',
    name: '테스터',
    role: 'USER',
    certificationStatus: 'NONE',
    bootcampName: undefined,
    bootcampGeneration: undefined,
    graduationDate: undefined,
    certificateUrl: undefined,
    createdAt: '2026-04-07T00:00:00',
    updatedAt: '2026-04-07T00:00:00',
    ...overrides,
  };
}
