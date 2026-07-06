import { JwtPayload } from './jwt-auth.guard';

declare module 'express' {
  interface Request {
    user?: JwtPayload;
  }
}
