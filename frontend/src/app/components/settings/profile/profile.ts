import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/AuthService';
import { UserService } from '../../../core/services/UserService';

@Component({
  selector: 'app-profile',
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
/**
 * Profile settings tab: lets the user view and edit their name/email, and
 * permanently delete their own account.
 */
export class ProfileComponent implements OnInit {
  private formBuilder = inject(FormBuilder);
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private router = inject(Router);

  // Reactive form holding the editable profile fields
  form: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
  });

  // UI feedback signals
  successMessage = signal('');
  errorMessage = signal('');
  // True while an account deletion is in flight (disables the button)
  deleting = signal(false);

  // Id of the currently authenticated user
  private userId = this.authService.getUserId();

  /** Lifecycle hook: pre-fill the form with the current user's data. */
  ngOnInit(): void {
    // Nothing to load without a known user id
    if (!this.userId) return;
    this.userService.getUserById(this.userId).subscribe({
      next: (user) => this.form.patchValue({ name: user.name, email: user.email }),
      error: () => this.errorMessage.set('Could not load your profile.'),
    });
  }

  /** Saves the edited profile fields and refreshes the cached name. */
  onSave(): void {
    // Guard against invalid form or missing user id
    if (this.form.invalid || !this.userId) return;
    // Reset previous feedback before the new attempt
    this.successMessage.set('');
    this.errorMessage.set('');

    this.userService.updateUserData(this.userId, this.form.value).subscribe({
      next: (user) => {
        // Keep the locally cached name in sync for the avatar/header
        this.authService.saveName(user.name);
        this.successMessage.set('Profile updated.');
      },
      error: () => this.errorMessage.set('Could not save your changes.'),
    });
  }

  /** Permanently deletes the user's account after confirmation, then logs out. */
  onDelete(): void {
    if (!this.userId) return;
    // Require an explicit confirmation for this irreversible action
    const confirmed = confirm('Permanently delete your account and all associated data?');
    if (!confirmed) return;

    this.deleting.set(true);
    this.userService.deleteUser(this.userId).subscribe({
      next: () => {
        // Account gone: clear the session and return to login
        this.authService.logout();
        this.router.navigate(['/login']);
      },
      error: () => {
        // Re-enable the button and report the failure
        this.deleting.set(false);
        this.errorMessage.set('Could not delete your account.');
      },
    });
  }
}
