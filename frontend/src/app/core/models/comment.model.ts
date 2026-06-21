/** A comment as returned by the backend (GET/POST /tickets/{id}/comments). */
export interface Comment {
  id: string;
  content: string;
  ticketId: string;
  authorId: string;
  authorName: string;
  authorRole: string;
  createdAt: string;
}

/** Payload for adding a comment to a ticket. */
export interface CommentRequest {
  content: string;
}
