import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environement } from '../environements/environements';
import { UpdateUserDataRequest, Users, UpdateUserRoleRequest } from '../models/user.model';
import { Observable } from 'rxjs';

/**
 * Client-side gateway to the user-related REST endpoints.
 */
@Injectable({
  providedIn: 'root',
})
export class UserService {
  // Base API URL taken from the active environment
  private readonly apiUrl = environement.apiUrl;

  constructor (
    private readonly http: HttpClient
  ) {}

  /**
   * Fetches every user.
   * @returns an Observable emitting the list of users
   */
  getAllUsers(){
    return this.http.get<Users[]> (`${this.apiUrl}/users/all`);
  }

  /**
   * Fetches a single user by id.
   * @param id the user's unique identifier
   * @returns an Observable emitting the matching user
   */
  getUserById(id: string){
    return this.http.get<Users> (`${this.apiUrl}/users/${id}`);
  }

  /**
   * Updates a user's editable data (name, email, password).
   * @param id the user's unique identifier
   * @param request the fields to update
   * @returns an Observable emitting the updated user
   */
  updateUserData(id: string, request: UpdateUserDataRequest){
    return this.http.patch<Users> (`${this.apiUrl}/users/${id}`, request);
  }

  /**
   * Updates a user's role (admin-only endpoint).
   * @param id the user's unique identifier
   * @param request the new role to assign
   * @returns an Observable emitting the updated user
   */
  updateUserRole(id: string, request: UpdateUserRoleRequest){
    return this.http.patch<Users> (`${this.apiUrl}/users/admin/${id}/role`, request);
  }

  /**
   * Deletes a user by id.
   * @param id the user's unique identifier
   * @returns an Observable completing when the user is deleted
   */
  deleteUser( id: string): Observable<void>{
    return this.http.delete<void> (`${this.apiUrl}/users/${id}`);
  }

}
