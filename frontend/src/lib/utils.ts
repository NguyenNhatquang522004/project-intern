import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

// Standard cn utility
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
