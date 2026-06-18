import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../../core/services/AuthService';
import { UserService } from '../../../core/services/UserService';

@Component({
  selector: 'app-password',
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
  ],
  templateUrl: './password.html',
  styleUrl: './password.scss',
})
/**
 * Password settings tab: lets the user change their password, validating that
 * the confirmation matches before submitting.
 */
export class PasswordComponent {
  private formBuilder = inject(FormBuilder);
  private authService = inject(AuthService);
  private userService = inject(UserService);

  // Reactive form with a cross-field validator ensuring both passwords match
  form: FormGroup = this.formBuilder.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(12)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: this.passwordsMatch },
  );

  // UI feedback signals
  successMessage = signal('');
  errorMessage = signal('');

  // Id of the currently authenticated user
  private userId = this.authService.getUserId();

  /**
   * Cross-field validator: checks that newPassword and confirmPassword match.
   * @param group the form group being validated
   * @returns null when they match, or a { mismatch: true } error otherwise
   */
  private passwordsMatch(group: AbstractControl): ValidationErrors | null {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return newPassword === confirmPassword ? null : { mismatch: true };
  }

  /** Submits the new password and resets the form on success. */
  onSubmit(): void {
    // Guard against invalid form or missing user id
    if (this.form.invalid || !this.userId) return;
    // Reset previous feedback before the new attempt
    this.successMessage.set('');
    this.errorMessage.set('');

    // Only the new password is sent to the backend
    const password = this.form.get('newPassword')?.value;
    this.userService.updateUserData(this.userId, { password }).subscribe({
      next: () => {
        this.successMessage.set('Password updated.');
        this.form.reset();
      },
      error: () => this.errorMessage.set('Could not update your password.'),
    });
  }
}
