/** Payload sent to the register endpoint. */
export interface RegisterRequest {
  name : string;
  email : string;
  password : string;
  role : string;
}


/** Payload sent to the login endpoint. */
export interface LoginRequest {
  email : string;
  password : string;
}

/** Response returned by the login/register/refresh endpoints. */
export interface AuthResponse {
  token: string;
  refreshToken: string;
  role: string;
  userId: string;
}

/** Payload sent to the refresh endpoint. */
export interface RefreshTokenRequest {
  refreshToken: string;
}
