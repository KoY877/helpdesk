import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { LoginComponent } from './components/login/login/login';
import { DashboardComponent } from './components/dashboard/dashboard';
import { RegisterComponent } from './components/register/register/register';
import { TicketBoardComponent } from './components/tickets/ticket-board/ticket-board';
import { UserBoardComponent } from './components/users/user-board/user-board';
import { SettingBoardComponent } from './components/settings/setting-board/setting-board';
import { ProfileComponent } from './components/settings/profile/profile';
import { PasswordComponent } from './components/settings/password/password';

/**
 * Application route table. Public routes (login/register) are open; every
 * feature route is protected by {@link authGuard}.
 */
export const routes: Routes = [
  // Public authentication routes
  {path:'register', component: RegisterComponent},
  {path:'login', component: LoginComponent},
  // Protected feature routes — require a valid session
  {path:'dashboard', component: DashboardComponent, canActivate: [authGuard]},
  {path:'tickets', component: TicketBoardComponent, canActivate: [authGuard]},
  {path:'users', component: UserBoardComponent, canActivate: [authGuard]},
  // Settings holds nested child routes, defaulting to the profile tab
  {path:'settings', component: SettingBoardComponent, canActivate: [authGuard],
    children: [
      {path:'profile', component: ProfileComponent},
      {path:'password', component: PasswordComponent},
      {path:'', redirectTo: 'profile', pathMatch: 'full'},
    ]
  },
  // Default and unknown routes fall back to login
  {path:'', redirectTo: 'login', pathMatch: 'full'},
  {path:'**', redirectTo: 'login'},
];
