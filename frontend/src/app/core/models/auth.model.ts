export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  userId: number;
  username: string;
  role: string;
  fullName: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  birthDate?: string;
  address?: string;
  bloodType?: string;
  captchaToken?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}