import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/AuthService';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatRow } from "@angular/material/table";


@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatCardModule, RouterModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
/**
 * Login screen: validates the credentials form and, on success, stores the
 * session data and navigates to the dashboard.
 */
export class LoginComponent {

  // Reactive form holding the email/password fields
  form : FormGroup;
  // Error message shown to the user on failed login
  errorMessage: string = '';

  constructor(
    private formBuilder : FormBuilder,
    private authService : AuthService,
    private router : Router
  ){
    // Build the form with required + email/format validators
    this.form = formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    })
  }

  /**
   * Submits the login form: authenticates, persists the session, then redirects.
   */
  onSubmit(): void {
    // Ignore submissions while the form is invalid
     if (this.form.invalid) return;

    this.authService.login(this.form.value).subscribe({
      next: (response) => {
        // Persist token, refresh token, role and user id for the session
        this.authService.saveToken(response.token);
        this.authService.saveRefreshToken(response.refreshToken);
        this.authService.saveRole(response.role);
        this.authService.saveUserId(response.userId);
        // Enter the authenticated area
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        // Generic message to avoid revealing which field was wrong
        this.errorMessage = 'Invalid email or password.';
      }
    })

  }
}
