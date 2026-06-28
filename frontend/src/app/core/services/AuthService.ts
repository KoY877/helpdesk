import { Injectable } from '@angular/core';
import { environement } from '../environements/environements';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest, RefreshTokenRequest, RegisterRequest } from '../models/auth.model';

/**
 * Handles authentication: calls the auth endpoints and persists the
 * session data (token, role, user info) in localStorage.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  // Base API URL taken from the active environment
  private readonly apiUrl = environement.apiUrl;
  // localStorage key under which the JWT is stored
  private readonly TOKEN_KEY = 'token';
  // localStorage key under which the refresh token is stored
  private readonly REFRESH_TOKEN_KEY = 'refreshToken';



  constructor (
    private readonly http: HttpClient
  ) { }

  // -------- Public Endpoints ---------------------------

  /**
   * Authenticates a user against the backend.
   * @param request the login credentials (email, password)
   * @returns an Observable emitting the auth response (token, role, user id)
   */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse> (`${this.apiUrl}/auth/login`, request);
  }

  /**
   * Registers a new user account.
   * @param request the registration data (name, email, password)
   * @returns an Observable emitting the auth response (token, role, user id)
   */
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse> (`${this.apiUrl}/auth/register`, request);
  }

  /**
   * Exchanges a refresh token for a new access token and refresh token.
   * @param refreshToken the refresh token currently stored for the session
   * @returns an Observable emitting the new auth response (token, refreshToken, role, user id)
   */
  refresh(refreshToken: string): Observable<AuthResponse> {
    const request: RefreshTokenRequest = { refreshToken };
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/refresh`, request);
  }

  /**
   * Persists the JWT in localStorage.
   * @param token the JWT returned by the backend
   */
  saveToken(token: string): void {
    localStorage.setItem('token', token);
  }

  /**
   * Persists the refresh token in localStorage.
   * @param refreshToken the refresh token returned by the backend
   */
  saveRefreshToken(refreshToken: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
  }

  /**
   * Reads the stored refresh token.
   * @returns the refresh token, or null if the user is not logged in
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Persists the user's role in localStorage.
   * @param role the role name (USER, AGENT, ADMIN)
   */
  saveRole(role: string){
    return localStorage.setItem('role', role);
  }

  /**
   * Reads the stored JWT.
   * @returns the token, or null if the user is not logged in
   */
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  /**
   * Reads the stored role.
   * @returns the role name, or null if none is stored
   */
  getRole(): string | null {
    return localStorage.getItem('role');
  }

  /**
   * Persists the authenticated user's id.
   * @param userId the user's unique identifier
   */
  saveUserId(userId: string): void {
    localStorage.setItem('userId', userId);
  }

  /**
   * Reads the stored user id.
   * @returns the user id, or null if none is stored
   */
  getUserId(): string | null {
      return localStorage.getItem('userId');
  }

  /**
   * Persists the authenticated user's name.
   * @param name the user's display name
   */
  saveName(name: string): void {
    localStorage.setItem('name', name);
  }

  /**
   * Reads the stored user name.
   * @returns the name, or null if none is stored
   */
  getName(): string | null {
    return localStorage.getItem('name');
  }

  /**
   * Tells whether a user is currently authenticated.
   * @returns true if a token is present in localStorage
   */
  isAuthenticated(): boolean {
    // A non-empty token is enough to consider the user logged in client-side
    return !!this.getToken();
  }

  /**
   * Clears all session data from localStorage, logging the user out.
   */
  logout(): void {
    const refreshToken = this.getRefreshToken();

    // Best-effort server-side revocation: the local session is cleared regardless of the outcome
    if (refreshToken) {
      const request: RefreshTokenRequest = { refreshToken };
      this.http.post(`${this.apiUrl}/auth/logout`, request).subscribe({ error: () => {} });
    }

    // Remove every piece of session data we persisted at login
    localStorage.removeItem('token');
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
    localStorage.removeItem('name');
    localStorage.removeItem('email');
  }
}
