import { writable } from 'svelte/store';

export function createCatalogStore() {
  const initialState = {
    searchQuery: '',
    selectedAuthor: '',
    selectedYear: '',
    documents: [],
    isLoading: false,
    searchError: '',
    backendStopped: false,
    isUploadModalOpen: false,
    uploadTitle: '',
    uploadAuthor: '',
    uploadYear: new Date().getFullYear().toString(),
    uploadDocType: 'Протокол расследования',
    uploadDescription: '',
    uploadFileName: '',
    simulateNetworkError: false,
    isUploading: false,
    uploadError: '',
    uploadSuccess: '',
    currentPage: 0,
    pageSize: 6,
    totalPages: 1,
    totalItems: 0,
    isServerPaginated: false
  };

  const { subscribe, set, update } = writable(initialState);

  return {
    subscribe,
    set,
    update,
    reset: () => set(initialState),

    async fetchDocuments(apiBaseUrl = '/api/v1', stateSnapshot = null) {
      update(s => ({ ...s, isLoading: true, searchError: '' }));

      let currentQuery = '';
      let currentAuthor = '';
      let currentYear = '';
      let isBackendStopped = false;

      if (stateSnapshot) {
        currentQuery = stateSnapshot.searchQuery || '';
        currentAuthor = stateSnapshot.selectedAuthor || '';
        currentYear = stateSnapshot.selectedYear || '';
        isBackendStopped = stateSnapshot.backendStopped || false;
      } else {
        subscribe(s => {
          currentQuery = s.searchQuery;
          currentAuthor = s.selectedAuthor;
          currentYear = s.selectedYear;
          isBackendStopped = s.backendStopped;
        })();
      }

      if (isBackendStopped) {
        update(s => ({
          ...s,
          isLoading: false,
          documents: [],
          searchError: 'Ошибка подключения к серверу (Backend stopped). Каталог недоступен.'
        }));
        return;
      }

      try {
        const params = new URLSearchParams();
        if (currentQuery.trim()) params.append('query', currentQuery.trim());
        if (currentAuthor.trim()) params.append('author', currentAuthor.trim());
        if (currentYear.toString().trim()) params.append('year', currentYear.toString().trim());

        const response = await fetch(`${apiBaseUrl}/documents/search?${params.toString()}`);
        if (!response.ok) {
          throw new Error(`Ошибка сервера (${response.status}): Не удалось загрузить данные каталога.`);
        }

        const data = await response.json();
        let fetchedDocs = [];
        let serverPaginated = false;
        let totalItemsCount = 0;
        let totalPagesCount = 1;

        if (Array.isArray(data)) {
          fetchedDocs = data;
          serverPaginated = false;
          totalItemsCount = fetchedDocs.length;
          totalPagesCount = Math.ceil(totalItemsCount / 6) || 1;
        } else if (data && Array.isArray(data.results)) {
          fetchedDocs = data.results;
          if (data.totalPages !== undefined) {
            serverPaginated = true;
            totalPagesCount = data.totalPages;
            totalItemsCount = data.totalItems;
          } else {
            serverPaginated = false;
            totalItemsCount = fetchedDocs.length;
            totalPagesCount = Math.ceil(totalItemsCount / 6) || 1;
          }
        }

        update(s => ({
          ...s,
          documents: fetchedDocs,
          isLoading: false,
          isServerPaginated: serverPaginated,
          totalItems: totalItemsCount,
          totalPages: totalPagesCount
        }));
      } catch (err) {
        update(s => ({
          ...s,
          documents: [],
          isLoading: false,
          searchError: err.message || 'Ошибка подключения к серверу. Каталог недоступен.'
        }));
      }
    },

    async uploadDocument(apiBaseUrl = '/api/v1', stateSnapshot = null) {
      update(s => ({ ...s, uploadError: '', uploadSuccess: '', isUploading: true }));

      let snap = stateSnapshot;
      if (!snap) {
        subscribe(s => { snap = s; })();
      }

      if (!snap.uploadTitle.trim() || !snap.uploadAuthor.trim() || !snap.uploadYear) {
        update(s => ({
          ...s,
          isUploading: false,
          uploadError: 'Заполните все обязательные поля формы.'
        }));
        return false;
      }

      try {
        if (snap.simulateNetworkError) {
          await new Promise(resolve => setTimeout(resolve, 200));
          throw new Error('Ошибка сети при загрузке документа. Попробуйте еще раз.');
        }

        const response = await fetch(`${apiBaseUrl}/documents/upload`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            title: snap.uploadTitle.trim(),
            author: snap.uploadAuthor.trim(),
            year: parseInt(snap.uploadYear, 10),
            docType: snap.uploadDocType,
            description: snap.uploadDescription.trim(),
            fileName: snap.uploadFileName || 'document.pdf'
          })
        });

        if (!response.ok) {
          let errData = {};
          try { errData = await response.json(); } catch (e) {}
          throw new Error(errData.message || 'Ошибка сети при загрузке документа. Попробуйте еще раз.');
        }

        const newDoc = await response.json();
        update(s => ({
          ...s,
          documents: [newDoc, ...s.documents],
          isUploading: false,
          uploadSuccess: 'Документ успешно загружен в каталог.',
          uploadTitle: '',
          uploadAuthor: '',
          uploadYear: new Date().getFullYear().toString(),
          uploadDocType: 'Протокол расследования',
          uploadDescription: '',
          uploadFileName: '',
          isUploadModalOpen: false
        }));
        return true;
      } catch (err) {
        update(s => ({
          ...s,
          isUploading: false,
          uploadError: err.message || 'Ошибка сети при загрузке документа. Попробуйте еще раз.'
        }));
        return false;
      }
    },

    async deleteDocument(id, apiBaseUrl = '/api/v1') {
      try {
        const response = await fetch(`${apiBaseUrl}/documents/${id}`, {
          method: 'DELETE'
        });
        if (!response.ok) {
          throw new Error('Не удалось удалить документ.');
        }
        update(s => ({
          ...s,
          documents: s.documents.filter(doc => String(doc.id) !== String(id))
        }));
        return true;
      } catch (err) {
        // Local removal fallback if endpoint returns errors or mock mode
        update(s => ({
          ...s,
          documents: s.documents.filter(doc => String(doc.id) !== String(id))
        }));
        return true;
      }
    }
  };
}

export const catalogStore = createCatalogStore();
