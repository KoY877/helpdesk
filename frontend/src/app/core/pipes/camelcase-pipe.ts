import { Pipe, PipeTransform } from '@angular/core';

/**
 * Pipe that capitalizes the first letter of a string and lowercases the rest
 * (e.g. "IN_PROGRESS" -> "In_progress", "admin" -> "Admin").
 */
@Pipe({
  name: 'camelcase',
})
export class CamelcasePipe implements PipeTransform {

  /**
   * Transforms the input string to a capitalized form.
   * @param value the string to transform
   * @returns the capitalized string, or the original value when it is falsy
   */
   transform(value?: string): string | undefined {
    // Only transform non-empty strings; pass through null/undefined/empty
    if (value)
      return value.charAt(0).toUpperCase()+value.slice(1).toLowerCase();
    return value
  }

}
