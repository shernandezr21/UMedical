import { AbstractControl } from '@angular/forms';

/** Mensaje de error de un campo, o null si no hay que mostrar nada (todavia no lo toco o es valido). */
export function controlErrorText(group: AbstractControl, name: string): string | null {
  const control = group.get(name);
  if (!control || !control.invalid || !(control.touched || control.dirty)) {
    return null;
  }
  const errors = control.errors ?? {};
  if (errors['required']) return 'Este campo es obligatorio.';
  if (errors['email']) return 'Ingresa un correo valido.';
  if (errors['minlength']) return `Minimo ${errors['minlength'].requiredLength} caracteres.`;
  if (errors['maxlength']) return `Maximo ${errors['maxlength'].requiredLength} caracteres.`;
  if (errors['pattern']) return 'Formato invalido (solo letras, numeros, punto, guion y guion bajo).';
  return 'Valor invalido.';
}