
/** A user as returned by the backend. */
export interface Users {
  id: string;
  name: string;
  email: string;
  role: 'USER' | 'AGENT' | 'ADMIN';
  createdAt: string;
}

/** Payload for updating a user's editable fields (all optional). */
export interface UpdateUserDataRequest {
  name?: string;
  email?: string;
  password?: string;
}

/** Payload for updating a user's role. */
export interface UpdateUserRoleRequest {
  role : 'USER' | 'AGENT' | 'ADMIN';
}



