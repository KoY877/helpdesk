import { Component, Inject, OnInit, Optional, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { Ticket } from '../../../core/models/ticket.model';
import { Comment } from '../../../core/models/comment.model';
import { TicketService } from '../../../core/services/TicketService';

/** View model for a comment shown in the conversation thread. */
interface CommentView {
  authorName: string;
  authorRole: string;
  content: string;
  createdAt: string;
}

@Component({
  selector: 'app-comment',
  imports: [CommonModule, ReactiveFormsModule, MatIconModule],
  templateUrl: './comment.html',
  styleUrl: './comment.scss',
})
/**
 * Ticket detail / conversation view: shows the ticket summary, the comments
 * saved on the ticket and a composer to add a new one. Opened as a dialog with
 * the ticket injected through MAT_DIALOG_DATA.
 */
export class CommentComponent implements OnInit {

  // The ticket handed in by the opener; rendered in the header
  ticket: Ticket;
  // Comment thread shown under the ticket (reactive so new ones appear instantly)
  comments = signal<CommentView[]>([]);
  // Reactive form backing the composer textarea
  form: FormGroup;
  // Error message shown when posting a comment fails
  errorMessage = '';

  /**
   * @param formBuilder builds the composer form
   * @param ticketService loads existing comments and posts new ones
   * @param dialogRef handle used to close the dialog (optional outside a dialog)
   * @param data dialog payload carrying the ticket to display
   */
  constructor(
    private formBuilder: FormBuilder,
    private ticketService: TicketService,
    @Optional() private dialogRef: MatDialogRef<CommentComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { ticket: Ticket },
  ) {
    // Expose the injected ticket to the template
    this.ticket = data.ticket;
    // The composer requires non-empty content, matching the backend @NotBlank
    this.form = formBuilder.group({
      content: ['', [Validators.required, Validators.maxLength(1000)]],
    });
  }

  /** Lifecycle hook: load the comments already saved on the ticket. */
  ngOnInit(): void {
    this.loadComments();
  }

  /** Fetches the ticket's comments and renders them in the conversation. */
  private loadComments(): void {
    this.ticketService.getTicketComments(this.ticket.id).subscribe({
      // Map each backend comment to the conversation view model
      next: (comments) => this.comments.set(comments.map(c => this.toView(c))),
      error: (err) => console.error(err),
    });
  }

  /**
   * Adapts a backend comment to the view model used by the template.
   * @param comment the comment returned by the API
   * @returns the matching conversation view model
   */
  private toView(comment: Comment): CommentView {
    return {
      authorName: comment.authorName,
      authorRole: comment.authorRole,
      content: comment.content,
      createdAt: comment.createdAt,
    };
  }

  /**
   * Builds the two-letter avatar initials from a display name.
   * @param name the author's display name
   * @returns up to two uppercase initials
   */
  initials(name: string): string {
    // Take the first letter of the first two words
    return name
      .split(' ')
      .map(part => part.charAt(0))
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }

  /** Closes the dialog and returns to the ticket list. */
  close(): void {
    this.dialogRef?.close();
  }

  /** Posts the typed comment and appends it to the thread on success. */
  onSave(): void {
    // Ignore submissions while the form is invalid
    if (this.form.invalid) return;

    const content = this.form.value.content as string;

    this.ticketService.commentTicket(this.ticket.id, { content }).subscribe({
      next: (created) => {
        // Append the freshly created comment to the bottom of the thread
        this.comments.update(list => [...list, this.toView(created)]);
        // Clear the composer for the next comment
        this.form.reset({ content: '' });
        this.errorMessage = '';
      },
      error: () => {
        this.errorMessage = 'Failed to post comment. Please try again.';
      },
    });
  }
}
