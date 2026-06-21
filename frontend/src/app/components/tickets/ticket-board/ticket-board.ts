import { Component, signal, ViewChild } from '@angular/core';
import { Ticket, TicketAssignRequest, TicketStatusRequest } from '../../../core/models/ticket.model';
import { MatPaginator} from '@angular/material/paginator';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { AuthService } from '../../../core/services/AuthService';
import { TicketService } from '../../../core/services/TicketService';
import { MatDialog } from '@angular/material/dialog';
import { TicketDialogComponent } from '../ticket-dialog/ticket-dialog';
import { MatIcon } from "@angular/material/icon";
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatMenuModule } from "@angular/material/menu";
import { UserService } from '../../../core/services/UserService';
import { Users } from '../../../core/models/user.model';
import { MatButtonModule } from '@angular/material/button';
import { CommentComponent } from '../comment/comment';

@Component({
  selector: 'app-ticket-board',
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatTableModule, MatIcon, MatPaginator, MatMenuModule, MatInputModule, MatFormFieldModule],
  templateUrl: './ticket-board.html',
  styleUrl: './ticket-board.scss',
})
/**
 * Ticket board: a richer ticket table for agents/admins, with status filtering,
 * assignment to assignable users, status transitions and deletion.
 */
export class TicketBoardComponent {
  // Paginator wired to the table after the view initializes
 @ViewChild(MatPaginator) paginator!: MatPaginator;

  // Columns rendered by the Material table
  displayedColumns: string[] = ['title', 'status', 'assignedTo', 'createdBy', 'createdAt', 'Actions'];
  // Current user's role, drives which actions are available
  role: string | null = null;
  // Currently active status filter (empty means "all")
  activeFilter: string = '';
  // Users (agents/admins) a ticket can be assigned to
  assignableUsers = signal<Users[]>([]);



  /**
   * Builds the CSS class used to colour a role badge.
   * @param role the user role
   * @returns the matching CSS class name
   */
  roleClass(role: string): string {
    return 'role-' + role;
  }

  /**
   * Builds the CSS class used to colour a status cell.
   * @param status the ticket status
   * @returns the matching CSS class name
   */
  statusClass(status: string): string {
    return 'status-' + status;
  }

  /**
   * Records and applies a status filter chosen from the UI.
   * @param filter the status to filter by
   */
  setFilter(filter: string): void {
    this.activeFilter = filter;
    this.applyFilter(filter);
  }

  // Table data source and reactive counters for each status bucket
  dataSource = new MatTableDataSource<Ticket>([]);
  countTotal = signal(0);
  countIsOpen = signal(0);
  countIsInProgress = signal(0);
  countIsResolved = signal(0);

  constructor(
    private authService: AuthService,
    private ticketService: TicketService,
    private userService: UserService,
    private dialog: MatDialog,
  ) {}

  /** Lifecycle hook: load role, tickets and (for agents/admins) assignable users. */
  ngOnInit(): void {
    this.role = this.authService.getRole();
    this.loadTickets();

    // Only privileged roles need the list of possible assignees
    if (this.role === 'AGENT' || this.role === 'ADMIN') {
      this.loadAssignableUsers();
    }
  }

  /** Lifecycle hook: attach the paginator once the view is ready. */
  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  /**
   * Filters the table either from a search event or a direct status string.
   * @param eventOrStatus an input event or a raw status string
   */
  applyFilter(eventOrStatus: Event | string) {

    // Accept both a raw string (status chips) and an input event (search box)
    const filterValue = typeof eventOrStatus === 'string'
      ? eventOrStatus
      : (eventOrStatus.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  /** Opens the create-ticket dialog and reloads the list if one was created. */
  openDialog(): void {
    const dialogRef = this.dialog.open(TicketDialogComponent, { width: '500px' });
    dialogRef.afterClosed().subscribe(result => {
      // A truthy result means a ticket was created
      if (result) this.loadTickets();
    });
  }

  /**
   * Opens the comment dialog for a given ticket. The ticket is forwarded to the
   * dialog through Angular Material's `data` option so it can be displayed there.
   * @param ticket the ticket whose comments are being viewed/added
   */
  openDialogComment(ticket: Ticket): void {
    const dialogRef = this.dialog.open(CommentComponent, {
      width: '720px',
      maxWidth: '95vw',
      // Custom panel class strips the default white padding for the dark design
      panelClass: 'comment-dialog',
      // Pass the selected ticket to CommentComponent via MAT_DIALOG_DATA
      data: { ticket },
    });
    dialogRef.afterClosed().subscribe(result => {
      // A truthy result means a comment was added; refresh the list
      if (result) this.loadTickets();
    });
  }

  /** Loads the visible tickets, sorts them newest-first and refreshes counters. */
  loadTickets(): void {
    this.ticketService.getVisibleTickets().subscribe({
      next: (tickets) => {
        // Sort a copy by creation date, most recent first
        this.dataSource.data = [...tickets].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        // Recompute the status counters shown above the table
        this.countTotal.set(tickets.length);
        this.countIsOpen.set(tickets.filter(t => t.status === 'OPEN').length);
        this.countIsInProgress.set(tickets.filter(t => t.status === 'IN_PROGRESS').length);
        this.countIsResolved.set(tickets.filter(t => t.status === 'RESOLVED').length);
      },
      error: (err) => console.error(err),
    });
  }

  /**
   * Moves a ticket to IN_PROGRESS.
   * @param id the ticket id
   */
  handleStatus(id: string): void {
    const request: TicketStatusRequest = { status: 'IN_PROGRESS' };

    this.ticketService.updateTicketStatus(request, id).subscribe({
      next: () => this.loadTickets(),
      error: (err) => console.error(err)
    });
  }

  /**
   * Transitions a ticket to an arbitrary target status.
   * @param id the ticket id
   * @param status the target status
   */
  handleTransition(id: string, status: string): void {
    const request: TicketStatusRequest = { status };

    this.ticketService.updateTicketStatus(request, id).subscribe({
      next: () => this.loadTickets(),
      error: (err) => console.error(err)
    });
  }

  /** Loads the users (agents/admins) eligible to be assigned tickets. */
  private loadAssignableUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        // Keep only privileged users; plain USERs cannot be assignees
        this.assignableUsers.set(
          users.filter(u => u.role === 'AGENT' || u.role === 'ADMIN')
        );
      },
      error: (err) => console.error(err),
    });
  }

  /**
   * Assigns a ticket to a user.
   * @param ticketId the ticket id
   * @param assigneeId the id of the user to assign it to
   */
  handleAssign(ticketId: string, assigneeId: string): void {
    const requestAssign: TicketAssignRequest = { assignedToId: assigneeId };

    this.ticketService.assignTicket(ticketId, requestAssign).subscribe({
      next: () => this.loadTickets(),
      error: (err) => console.error(err)
    });


  }

  /**
   * Deletes a ticket and refreshes the list.
   * @param id the ticket id
   */
  handleDelete(id: string): void {
    this.ticketService.deleteTicket(id).subscribe({
      next: () => this.loadTickets(),
      error: (err) => console.error(err),
    });
  }
}
