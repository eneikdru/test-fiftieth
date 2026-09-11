import { writable } from 'svelte/store';

export function createPrivacyStore() {
  const initialState = {
    privacyFormat: 'ZIP',
    privacyNotes: '',
    isExportingPrivacy: false,
    privacyExportSuccess: '',
    privacyExportError: '',
    exportJob: null,

    isPrivacyDeleteModalOpen: false,
    erasureReason: 'Отозвано согласие на обработку персональных данных (152-ФЗ)',
    erasureScope: 'ALL_PERSONAL_DATA',
    confirmationInput: '',
    isDeletingPrivacy: false,
    privacyDeleteSuccess: '',
    privacyDeleteError: '',
    erasureJob: null
  };

  const { subscribe, set, update } = writable(initialState);

  return {
    subscribe,
    set,
    update,
    reset: () => set(initialState),

    async requestDataExport(currentUser, apiBaseUrl = '/api/v1', stateSnapshot = null) {
      update(s => ({ ...s, isExportingPrivacy: true, privacyExportError: '', privacyExportSuccess: '' }));

      let snap = stateSnapshot;
      if (!snap) {
        subscribe(s => { snap = s; })();
      }

      const subjectId = currentUser?.username || currentUser?.id || 'usr_101';

      try {
        const response = await fetch(`${apiBaseUrl}/privacy/export-requests`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            subject_id: subjectId,
            requested_format: snap.privacyFormat,
            notes: snap.privacyNotes ? snap.privacyNotes.trim() : ''
          })
        });

        if (!response.ok) {
          let errData = {};
          try { errData = await response.json(); } catch (e) {}
          throw new Error(errData.message || 'Ошибка при запросе экспорта данных. Попробуйте позже.');
        }

        const job = await response.json();
        update(s => ({
          ...s,
          isExportingPrivacy: false,
          exportJob: job,
          privacyExportSuccess: 'Запрос на экспорт данных успешно создан и обрабатывается.'
        }));
        return job;
      } catch (err) {
        update(s => ({
          ...s,
          isExportingPrivacy: false,
          privacyExportError: err.message || 'Произошла ошибка при формировании запроса экспорта.'
        }));
        return null;
      }
    },

    async requestAccountErasure(currentUser, apiBaseUrl = '/api/v1', stateSnapshot = null) {
      update(s => ({ ...s, isDeletingPrivacy: true, privacyDeleteError: '', privacyDeleteSuccess: '' }));

      let snap = stateSnapshot;
      if (!snap) {
        subscribe(s => { snap = s; })();
      }

      const subjectId = currentUser?.username || currentUser?.id || 'usr_101';
      const expectedToken = `УДАЛИТЬ ${subjectId}`;

      if (snap.confirmationInput.trim() !== expectedToken) {
        update(s => ({
          ...s,
          isDeletingPrivacy: false,
          privacyDeleteError: 'Код подтверждения введен неверно.'
        }));
        return false;
      }

      try {
        const response = await fetch(`${apiBaseUrl}/privacy/erasure-requests`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            subject_id: subjectId,
            confirmation_token: `CONFIRM_ERASURE_${subjectId}`,
            reason: snap.erasureReason,
            erasure_scope: snap.erasureScope
          })
        });

        if (!response.ok) {
          let errData = {};
          try { errData = await response.json(); } catch (e) {}
          throw new Error(errData.message || 'Ошибка при обработке запроса на удаление данных.');
        }

        const job = await response.json();
        update(s => ({
          ...s,
          isDeletingPrivacy: false,
          erasureJob: job,
          privacyDeleteSuccess: 'Запрос на удаление данных принят. Аккаунт и персональные данные будут удалены.',
          isPrivacyDeleteModalOpen: false,
          confirmationInput: ''
        }));
        return true;
      } catch (err) {
        update(s => ({
          ...s,
          isDeletingPrivacy: false,
          privacyDeleteError: err.message || 'Не удалось выполнить запрос на удаление аккаунта.'
        }));
        return false;
      }
    }
  };
}

export const privacyStore = createPrivacyStore();
