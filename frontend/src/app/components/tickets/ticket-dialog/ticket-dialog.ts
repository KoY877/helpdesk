import { Component, Optional } from '@angular/core';
import { MatIcon } from "@angular/material/icon";
import { MatFormField, MatError, MatHint } from "@angular/material/form-field";
import { MatInputModule } from '@angular/material/input';
import { MatButton, MatIconButton } from "@angular/material/button";
import { MatDialogRef } from "@angular/material/dialog";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TicketService } from '../../../core/services/TicketService';

@Component({
  selector: 'app-ticket-dialog',
  imports: [
    ReactiveFormsModule,
    MatIcon, MatFormField, MatError, MatHint,
    MatInputModule, MatButton, MatIconButton,
  ],
  templateUrl: './ticket-dialog.html',
  styleUrl: './ticket-dialog.scss',
})
/**
 * Modal dialog used to create a new ticket. On success it closes returning the
 * created ticket so the caller can refresh its list.
 */
export class TicketDialogComponent {

  // Reactive form holding the title/description fields
  form: FormGroup;
  // Error message shown on a failed creation
  errorMessage: string = '';

  constructor(
    private formBuilder: FormBuilder,
    private ticketService: TicketService,
    @Optional() public dialogRef: MatDialogRef<TicketDialogComponent>
  ) {
    // Build the form with length constraints matching the backend
    this.form = formBuilder.group({
      title: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(1000)]],
    });
  }

  /**
   * Current length of the description, used by the character counter.
   * @returns the number of characters typed (0 when empty)
   */
  get descriptionLength(): number {
    return this.form.get('description')?.value?.length ?? 0;
  }

  /** Closes the dialog without creating anything. */
  close(): void {
    this.dialogRef?.close();
  }

  /** Submits the form: creates the ticket and closes the dialog with the result. */
  onSubmit(): void {
    // Ignore submissions while the form is invalid
    if (this.form.invalid) return;

    this.ticketService.createTicket(this.form.value).subscribe({
      // Return the created ticket to the opener
      next: (ticket) => this.dialogRef?.close(ticket),
      error: () => {
        this.errorMessage = 'Failed to create ticket. Please try again.';
      },
    });
  }

}
