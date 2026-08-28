export type UserRole = 'ADMIN' | 'OPERATOR'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  displayName: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface CurrentUser {
  id: string
  username: string
  displayName: string
  avatarUrl?: string | null
  roles: UserRole[]
}

export interface TokenResponse {
  tokenType: 'Bearer'
  accessToken: string
  expiresIn: number
  refreshToken: string
  refreshExpiresIn: number
  user: CurrentUser
}
