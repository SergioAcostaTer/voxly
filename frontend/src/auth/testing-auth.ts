import type { User } from '../types/auth'

export const AUTH_BYPASS_ENABLED = import.meta.env.VITE_AUTH_BYPASS === 'true'
export const TESTING_ACCESS_TOKEN = 'testing-bypass'

export const TESTING_USER: User = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'test@voxly.local',
  username: 'testuser',
  active: true,
  emailVerified: true,
  twoFactorEnabled: false,
  roles: ['User'],
  createdAt: new Date().toISOString(),
  modifiedAt: new Date().toISOString(),
}
