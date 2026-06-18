import { Component} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/AuthService';
import { CommonModule } from '@angular/common';
import { lastValueFrom } from 'rxjs';
import { Router, RouterModule } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatCardModule, RouterModule],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
/**
 * Registration screen: validates the sign-up form and, on success, stores the
 * session data and navigates to the dashboard.
 */
export class RegisterComponent {


  // Reactive form holding the name/email/password fields
  form: FormGroup;
  // Error message shown to the user on failed registration
  errorMessage: string = '';

  constructor(
    private formbuilder : FormBuilder,
    private authService : AuthService,
    private router : Router
  ){
    // Build the form with required, length and email validators
    this.form = formbuilder.group({
      name: ['', [Validators.required, Validators.minLength(5)]],
      email:['',[Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(12)]]
    })
  }

  /**
   * Submits the registration form: creates the account, persists the session,
   * then redirects to the dashboard.
   */
  onSubmit(): void {
    // Ignore submissions while the form is invalid
    if (this.form.invalid) return;

    this.authService.register(this.form.value).subscribe({
      next: (response) => {
        // Persist token, role and user id for the session
        this.authService.saveToken(response.token);
        this.authService.saveRole(response.role);
        this.authService.saveUserId(response.userId);
        // Enter the authenticated area
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        // Most likely cause is a duplicate email
        this.errorMessage = 'Registration failed. Email may already be in use.';
      }
    })

  }

}
