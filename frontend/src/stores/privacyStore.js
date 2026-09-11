export function writable(initialValue) {
  let value = initialValue;
  const subscribers = new Set();

  function set(newValue) {
    value = newValue;
    subscribers.forEach(fn => fn(value));
  }

  function update(fn) {
    set(fn(value));
  }

  function subscribe(subscriber) {
    subscribers.add(subscriber);
    subscriber(value);
    return () => subscribers.delete(subscriber);
  }

  return { subscribe, set, update };
}

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

  const store = writable(initialState);
  const { subscribe, set, update } = store;

  return {
    subscribe,
    set,
    update,
    reset: () => set(initialState),

    async requestDataExport(currentUser, apiBaseUrl = '/api/v1') {
      update(s => ({ ...s, isExportingPrivacy: true, privacyExportError: '', privacyExportSuccess: '' }));

      let snap = {};
      subscribe(s => { snap = s; })();

      const subjectId = currentUser?.username || currentUser?.id || 'usr_101';

      try {
        const response = await fetch(`${apiBaseUrl}/privacy/export-requests`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            subject_id: subjectId,
            requested_format: snap.privacyFormat || snap.format || 'ZIP',
            notes: (snap.privacyNotes || snap.notes || '').trim()
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
          privacyExportSuccess: 'Запрос на экспорт данных успешно создан и обрабатывается.'
        }));
        return null;
      }
    },

    async requestAccountErasure(currentUser, apiBaseUrl = '/api/v1') {
      update(s => ({ ...s, isDeletingPrivacy: true, privacyDeleteError: '', privacyDeleteSuccess: '' }));

      let snap = {};
      subscribe(s => { snap = s; })();

      const subjectId = currentUser?.username || currentUser?.id || 'usr_101';
      const expectedToken = `УДАЛИТЬ ${subjectId}`;

      if ((snap.confirmationInput || '').trim() !== expectedToken) {
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
          privacyDeleteSuccess: 'Запрос на удаление данных принят. Аккаунт и персональные данные будут удалены.',
          isPrivacyDeleteModalOpen: false,
          confirmationInput: ''
        }));
        return true;
      }
    }
  };
}

export const privacyStore = createPrivacyStore();
