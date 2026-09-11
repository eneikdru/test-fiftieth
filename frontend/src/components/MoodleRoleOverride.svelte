<script>
  import { createEventDispatcher, onMount } from 'svelte';

  export let currentUser;
  export let getApiBaseUrl;
  let isSaving = false;
  let newRole = currentUser ? currentUser.role : 'RESEARCHER';

  let mappings = [];
  let isLoadingMappings = false;
  let mappingsError = '';

  const dispatch = createEventDispatcher();

  const roleLabelMap = {
    'ADMIN': 'Администратор (ADMIN)',
    'EPIDEMIOLOGIST': 'Эпидемиолог (EPIDEMIOLOGIST)',
    'RESEARCHER': 'Исследователь (RESEARCHER)',
    'USER': 'Пользователь (USER)'
  };

  function formatInternalRole(role) {
    return roleLabelMap[role] || role;
  }

  async function fetchRoleMappings() {
    isLoadingMappings = true;
    mappingsError = '';
    try {
      const token = localStorage.getItem('access_token');
      const headers = { 'Content-Type': 'application/json' };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const response = await fetch(`${getApiBaseUrl()}/auth/moodle/override-role`, {
        method: 'GET',
        headers
      });
      if (response.ok) {
        const data = await response.json();
        mappings = Array.isArray(data.mappings) ? data.mappings : [];
      } else {
        mappingsError = 'Не удалось загрузить иерархию ролей Moodle.';
      }
    } catch (error) {
      mappingsError = 'Ошибка подключения при загрузке иерархии ролей.';
    } finally {
      isLoadingMappings = false;
    }
  }

  async function saveRoleOverride() {
    isSaving = true;
    try {
      const token = localStorage.getItem('access_token');
      const headers = { 'Content-Type': 'application/json' };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const response = await fetch(`${getApiBaseUrl()}/auth/moodle/override-role`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ userId: currentUser ? currentUser.id : null, role: newRole })
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

  onMount(() => {
    fetchRoleMappings();
  });
</script>

<div class="mt-4 p-4 bg-surface-container-low border border-outline-variant rounded-lg space-y-4">
  <!-- Moodle Role Hierarchy Mappings Display -->
  <div>
    <div class="flex items-center justify-between mb-2">
      <h5 class="text-sm font-semibold text-on-surface">Иерархия ролей Moodle и правила сопоставления</h5>
      <button
        type="button"
        on:click={fetchRoleMappings}
        disabled={isLoadingMappings}
        class="text-xs font-medium text-primary hover:underline focus:outline-none focus:ring-2 focus:ring-primary/50 rounded px-1.5 py-0.5"
        aria-label="Обновить иерархию ролей Moodle"
      >
        Обновить
      </button>
    </div>

    {#if isLoadingMappings}
      <div role="status" class="py-3 text-xs text-on-surface-variant flex items-center gap-2">
        <span class="animate-spin inline-block w-3.5 h-3.5 border-2 border-primary border-t-transparent rounded-full" aria-hidden="true"></span>
        <span>Загрузка иерархии ролей...</span>
      </div>
    {:else if mappingsError}
      <div role="alert" class="p-2.5 rounded bg-error-container text-on-error-container text-xs border border-error/20 mb-2">
        {mappingsError}
      </div>
    {:else if mappings.length === 0}
      <p class="text-xs text-on-surface-variant italic py-2">
        Правила сопоставления ролей Moodle не найдены.
      </p>
    {:else}
      <div class="overflow-x-auto border border-outline-variant rounded bg-surface">
        <table class="w-full text-left text-xs border-collapse" aria-label="Таблица сопоставления ролей Moodle">
          <thead class="bg-surface-variant text-on-surface font-semibold border-b border-outline-variant">
            <tr>
              <th scope="col" class="py-2 px-3">Паттерн роли Moodle</th>
              <th scope="col" class="py-2 px-3">Внутренняя роль архива</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/40 text-on-surface">
            {#each mappings as mapping}
              <tr class="hover:bg-surface-container-lowest transition-colors">
                <td class="py-2 px-3 font-mono text-[11px] text-primary font-medium">{mapping.moodle_role_pattern}</td>
                <td class="py-2 px-3 font-medium">{formatInternalRole(mapping.internal_role)}</td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {/if}
  </div>

  <hr class="border-outline-variant/60" />

  <!-- Individual User Role Override Section -->
  <div>
    <h5 class="text-sm font-semibold text-on-surface mb-2">Индивидуальное переопределение роли пользователя</h5>
    <div class="flex items-center gap-2">
      <select
        aria-label="Выберите роль для пользователя"
        class="flex-grow h-10 px-3 bg-surface border border-outline-variant rounded text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary"
        bind:value={newRole}
      >
        <option value="ADMIN">Администратор (ADMIN)</option>
        <option value="EPIDEMIOLOGIST">Эпидемиолог (EPIDEMIOLOGIST)</option>
        <option value="RESEARCHER">Исследователь (RESEARCHER)</option>
        <option value="USER">Пользователь (USER)</option>
      </select>
      <button
        type="button"
        class="h-10 px-4 bg-primary text-on-primary text-sm rounded font-medium hover:bg-on-primary-fixed-variant focus:outline-none focus:ring-2 focus:ring-primary/50 transition-colors disabled:opacity-60"
        on:click={saveRoleOverride}
        disabled={isSaving}
      >
        {isSaving ? 'Сохранение...' : 'Сохранить'}
      </button>
    </div>
  </div>
</div>
