<script>
  import { createEventDispatcher } from 'svelte';

  export let currentUser;
  export let getApiBaseUrl;
  let isSaving = false;
  let newRole = currentUser.role;

  const dispatch = createEventDispatcher();

  async function saveRoleOverride() {
    isSaving = true;
    try {
      const response = await fetch(`${getApiBaseUrl()}/auth/moodle/override-role`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: currentUser.id, role: newRole })
      });
      if (response.ok) {
        dispatch('roleUpdated', newRole);
        console.log('Role overridden successfully to:', newRole);
      } else {
        console.error('Failed to override role');
      }
    } catch (error) {
      console.error('Error overriding role:', error);
    } finally {
      isSaving = false;
    }
  }
</script>

<div class="mt-4 p-4 bg-surface-container-low border border-outline-variant rounded-lg">
  <h5 class="text-sm font-semibold text-on-surface mb-2">Управление ролями Moodle (Override)</h5>
  <div class="flex items-center gap-2">
    <select
      aria-label="Выберите роль для пользователя"
      class="flex-grow h-10 px-3 bg-surface border border-outline-variant rounded text-sm text-on-surface focus:outline-none focus:border-primary"
      bind:value={newRole}
    >
      <option value="ADMIN">Администратор</option>
      <option value="SENIOR_RESEARCHER">Старший научный сотрудник</option>
      <option value="RESEARCHER">Исследователь</option>
    </select>
    <button
      type="button"
      class="h-10 px-4 bg-primary text-on-primary text-sm rounded font-medium hover:bg-on-primary-fixed-variant transition-colors disabled:opacity-60"
      on:click={saveRoleOverride}
      disabled={isSaving}
    >
      {isSaving ? 'Сохранение...' : 'Сохранить'}
    </button>
  </div>
</div>
