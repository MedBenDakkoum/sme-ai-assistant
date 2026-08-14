import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService, AdminDocument } from '../../core/services/admin.service';

@Component({
  selector: 'app-admin-documents',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin.html',
  styleUrl: './admin.css'
})
export class AdminDocuments implements OnInit {

  documents: AdminDocument[] = [];
  selectedFile: File | null = null;
  isUploading = false;
  errorMessage = '';

  constructor(
    private adminService: AdminService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.adminService.getDocuments().subscribe({
      next: (docs) => {
        this.documents = docs;
        this.errorMessage = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[Admin] Échec chargement documents:', err);
        this.errorMessage = 'Impossible de charger la liste des documents.';
        this.cdr.detectChanges();
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    if (!file) {
      this.selectedFile = null;
      return;
    }
    const name = file.name.toLowerCase();
    if (!name.endsWith('.pdf') && !name.endsWith('.txt')) {
      this.selectedFile = null;
      this.errorMessage = 'Seuls les fichiers .pdf et .txt sont acceptés.';
      return;
    }
    this.selectedFile = file;
    this.errorMessage = '';
  }

  upload(): void {
    if (!this.selectedFile || this.isUploading) {
      return;
    }
    this.isUploading = true;
    this.errorMessage = '';
    this.adminService.uploadDocument(this.selectedFile).subscribe({
      next: () => {
        this.isUploading = false;
        this.selectedFile = null;
        this.cdr.detectChanges();
        const input = document.getElementById('file-input') as HTMLInputElement | null;
        if (input) {
          input.value = '';
        }
        this.loadDocuments();
      },
      error: (err: any) => {
        this.isUploading = false;
        const msg = err?.error?.error;
        this.errorMessage = msg
          ? `Échec de l'upload : ${msg}`
          : "Échec de l'upload. Vérifiez que le fichier est valide.";
        console.error('[Admin] Échec upload:', err);
        this.cdr.detectChanges();
      }
    });
  }

  deleteDocument(doc: AdminDocument): void {
    const confirmed = window.confirm(`Supprimer le document « ${doc.filename} » ?`);
    if (!confirmed) {
      return;
    }
    this.adminService.deleteDocument(doc.id).subscribe({
      next: () => {
        this.documents = this.documents.filter(d => d.id !== doc.id);
        this.errorMessage = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[Admin] Échec suppression:', err);
        this.errorMessage = `Impossible de supprimer « ${doc.filename} ».`;
        this.cdr.detectChanges();
      }
    });
  }

  typeLabel(doc: AdminDocument): string {
    const ct = (doc.contentType ?? '').toLowerCase();
    if (ct.includes('pdf')) {
      return 'PDF';
    }
    if (ct.includes('text')) {
      return 'TXT';
    }
    const ext = doc.filename.split('.').pop();
    return ext ? ext.toUpperCase() : '?';
  }

  statusLabel(doc: AdminDocument): string {
    return doc.indexed ? 'Indexé' : 'En cours';
  }

  badgeClass(doc: AdminDocument): string {
    return doc.indexed
      ? 'bg-accent/10 text-accent border-accent/20'
      : 'bg-accent-warm/10 text-accent-warm border-accent-warm/20';
  }

  formatDate(value: string): string {
    const date = new Date(value);
    if (isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
