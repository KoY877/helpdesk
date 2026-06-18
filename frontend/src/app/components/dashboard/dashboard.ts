import { Component, ViewChild, signal } from '@angular/core';
import { AuthService } from '../../core/services/AuthService';
import { Router } from '@angular/router';
import { Ticket, TicketStatusRequest } from '../../core/models/ticket.model';
import { TicketService } from '../../core/services/TicketService';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatCard, MatCardContent, MatCardHeader } from "@angular/material/card";
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TicketDialogComponent } from '../tickets/ticket-dialog/ticket-dialog';

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule, ReactiveFormsModule,
    MatTableModule, MatPaginatorModule, MatIconModule,
    MatCard, MatCardHeader, MatCardContent,
    MatFormFieldModule, MatInputModule,
    MatButtonModule, MatDialogModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
/**
 * Dashboard: shows the visible tickets in a paginated, filterable table along
 * with per-status counters, and lets the user act on tickets (assign,
 * transition, delete) or create a new one.
 */
export class DashboardComponent {
  // Paginator wired to the table after the view initializes
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  // Columns rendered by the Material table
  displayedColumns: string[] = ['order', 'title', 'description', 'status', 'createdAt', 'Actions'];
  // Current user's role, drives which actions are available
  role: string | null = null;

  /**
   * Builds the CSS class used to colour a status cell.
   * @param status the ticket status
   * @returns the matching CSS class name
   */
  statusClass(status: string): string {
    return 'status-' + status;
  }

  // Table data source and reactive counters for each status bucket
  dataSource = new MatTableDataSource<Ticket>([]);
  countTotal = signal(0);
  countIsOpen = signal(0);
  countIsInProgress = signal(0);
  countIsResolved = signal(0);

  constructor(
    private authService: AuthService,
    private router: Router,
    private ticketService: TicketService,
    private dialog: MatDialog,
  ) {}

  /** Lifecycle hook: read the role and load the tickets on init. */
  ngOnInit(): void {
    this.role = this.authService.getRole();
    this.loadTickets();
  }

  /** Lifecycle hook: attach the paginator once the view is ready. */
  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  /**
   * Filters the table from a search input event.
   * @param event the input event carrying the search text
   */
  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    // Material's filter expects a normalized, lowercased string
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
   * Moves a ticket to IN_PROGRESS (assignment shortcut).
   * @param id the ticket id
   */
  handleAssign(id: string): void {
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
    const request: TicketStatusRequest = { status: `${status}` };
    this.ticketService.updateTicketStatus(request, id).subscribe({
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
