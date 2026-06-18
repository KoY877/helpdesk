import { ChangeDetectorRef, Component, ViewChild } from '@angular/core';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { AuthService } from '../../../core/services/AuthService';
import { UserService } from '../../../core/services/UserService';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { UserDialogComponent } from '../user-dialog/user-dialog';
import { UpdateUserRoleRequest, Users } from '../../../core/models/user.model';
import { MatIconModule } from "@angular/material/icon";import { RouterModule } from "@angular/router";
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';

@Component({
  selector: 'app-user-board',
  imports: [CommonModule, ReactiveFormsModule,
    MatTableModule, MatPaginatorModule, MatIconModule,
    MatCardModule,
    MatFormFieldModule, MatInputModule,
    MatButtonModule, MatMenuModule, MatDialogModule, RouterModule],
  templateUrl: './user-board.html',
  styleUrl: './user-board.scss',
})
/**
 * User board (admin view): lists users in a paginated, filterable table and
 * lets an admin change a user's role or delete the account.
 */
export class UserBoardComponent {
  // Paginator wired to the table after the view initializes
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  // Columns rendered by the Material table
  displayedColumns: string[] = ['order', 'name', 'email', 'role', 'createdAt', 'actions'];
  // Current user's role
  role: string | null = null;
  // Currently active role filter (empty means "all")
  activeFilter: string = '';
  // Roles selectable when changing a user's role
  readonly roles: Array<'USER' | 'AGENT' | 'ADMIN'> = ['USER', 'AGENT', 'ADMIN'];

  /**
   * Builds the CSS class used to colour a role badge.
   * @param role the user role
   * @returns the matching CSS class name
   */
  roleClass(role: string): string {
    return 'role-' + role;
  }

  /**
   * Computes a user's two-letter initials for the avatar.
   * @param name the user's full name
   * @returns up to two uppercase initials
   */
  initialsFor(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  /**
   * Records and applies a role filter chosen from the UI.
   * @param filter the role to filter by
   */
  setFilter(filter: string): void {
    this.activeFilter = filter;
    this.applyFilter(filter);
  }

  // Table data source backing the user list
  dataSource = new MatTableDataSource<Users>([]);

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog,
  ) {}

  /** Lifecycle hook: read the role and load the users on init. */
  ngOnInit(): void {
    this.role = this.authService.getRole();
    this.loadUsers();
  }

  /** Lifecycle hook: attach the paginator once the view is ready. */
  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  /**
   * Filters the table either from a search event or a direct role string.
   * @param eventOrStatus an input event or a raw role string
   */
  applyFilter(eventOrStatus: Event | string) {

    // Accept both a raw string (role chips) and an input event (search box)
    const filterValue = typeof eventOrStatus === 'string'
      ? eventOrStatus
      : (eventOrStatus.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  /** Opens the user dialog and reloads the list if it reported a change. */
  openDialog(): void {
    const dialogRef = this.dialog.open(UserDialogComponent, { width: '500px' });
    dialogRef.afterClosed().subscribe(result => {
      if (result) this.loadUsers();
    });
  }

  /** Loads all users sorted newest-first. */
  loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        // Sort a copy by creation date, most recent first
        this.dataSource.data = [...users].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );

        // Force change detection since the update may run outside Angular's flow
        this.cdr.detectChanges();
      },
      error: (err) => console.error(err),
    });
  }

  /**
   * Changes a user's role and refreshes the list.
   * @param id the user id
   * @param role the new role to assign
   */
  handleRoleChange(id: string, role: 'USER' | 'AGENT' | 'ADMIN'): void {
    const request: UpdateUserRoleRequest = { role };
    this.userService.updateUserRole(id, request).subscribe({
      next: () => this.loadUsers(),
      error: (err) => console.error(err)
    });
  }

  /**
   * Deletes a user and refreshes the list.
   * @param id the user id
   */
  handleDelete(id: string): void {
    this.userService.deleteUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => console.error(err),
    });
  }
}
