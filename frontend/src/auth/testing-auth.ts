import type { User } from '../types/auth'

export const AUTH_BYPASS_ENABLED = import.meta.env.VITE_AUTH_BYPASS === 'true'
export const TESTING_ACCESS_TOKEN = 'testing-bypass'

const DEFAULT_TESTING_USER: User = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'test@voxly.local',
  username: 'testuser',
  active: true,
  emailVerified: true,
  twoFactorEnabled: false,
  professionalRole: 'Test speaker',
  coachingFocus: 'Practice sessions',
  roles: ['User'],
  createdAt: new Date().toISOString(),
  modifiedAt: new Date().toISOString(),
}

export const TESTING_USER: User = {
  ...DEFAULT_TESTING_USER,
  id: import.meta.env.VITE_AUTH_BYPASS_USER_ID || DEFAULT_TESTING_USER.id,
  email: import.meta.env.VITE_AUTH_BYPASS_EMAIL || DEFAULT_TESTING_USER.email,
  username: import.meta.env.VITE_AUTH_BYPASS_USERNAME || DEFAULT_TESTING_USER.username,
}
