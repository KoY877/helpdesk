import { Injectable } from '@angular/core';
import { environement } from '../environements/environements';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';

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
   * Persists the JWT in localStorage.
   * @param token the JWT returned by the backend
   */
  saveToken(token: string): void {
    localStorage.setItem('token', token);
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
    // Remove every piece of session data we persisted at login
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
    localStorage.removeItem('name');
    localStorage.removeItem('email');
  }
}
