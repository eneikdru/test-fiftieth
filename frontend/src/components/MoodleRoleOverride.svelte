<script>
  import { createEventDispatcher, onMount } from 'svelte';

  export let currentUser;
  export let getApiBaseUrl;
  let isSaving = false;
  let newRole = currentUser ? currentUser.role : 'RESEARCHER';

  let hierarchyLoading = false;
  let hierarchyError = null;
  let roleMappings = [];

  const dispatch = createEventDispatcher();

  const defaultMappings = [
    { id: 1, moodle_role_pattern: 'администратор / admin', internal_role: 'ADMIN' },
    { id: 2, moodle_role_pattern: 'старший научный сотрудник / эпидемиолог', internal_role: 'EPIDEMIOLOGIST' },
    { id: 3, moodle_role_pattern: 'исследователь / аспирант', internal_role: 'RESEARCHER' },
    { id: 4, moodle_role_pattern: 'пользователь / студент', internal_role: 'USER' }
  ];

  async function fetchRoleHierarchy() {
    hierarchyLoading = true;
    hierarchyError = null;
    try {
      const baseUrl = getApiBaseUrl ? getApiBaseUrl() : '/api/v1';
      const token = typeof localStorage !== 'undefined' ? localStorage.getItem('access_token') : null;
      const headers = { 'Content-Type': 'application/json' };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const response = await fetch(`${baseUrl}/auth/moodle/role-hierarchy`, { headers });
      if (response.ok) {
        const data = await response.json();
        if (data && Array.isArray(data.mappings)) {
          roleMappings = data.mappings;
        } else if (Array.isArray(data)) {
          roleMappings = data;
        } else {
          roleMappings = defaultMappings;
        }
      } else {
        // Fallback to default mappings on API error or offline mode
        roleMappings = defaultMappings;
      }
    } catch (err) {
      roleMappings = defaultMappings;
    } finally {
      hierarchyLoading = false;
    }
  }

  onMount(() => {
    fetchRoleHierarchy();
  });

  async function saveRoleOverride() {
    isSaving = true;
    try {
      const baseUrl = getApiBaseUrl ? getApiBaseUrl() : '/api/v1';
      const token = typeof localStorage !== 'undefined' ? localStorage.getItem('access_token') : null;
      const headers = { 'Content-Type': 'application/json' };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const response = await fetch(`${baseUrl}/auth/moodle/override-role`, {
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

  function getRoleTitle(role) {
    switch (role) {
      case 'ADMIN': return 'Администратор (ADMIN)';
      case 'EPIDEMIOLOGIST': return 'Эпидемиолог (EPIDEMIOLOGIST)';
      case 'RESEARCHER': return 'Исследователь (RESEARCHER)';
      case 'USER': return 'Пользователь (USER)';
      default: return role || '—';
    }
  }
</script>

<div class="mt-4 p-4 bg-surface-container-low border border-outline-variant rounded-lg space-y-4" id="moodle-role-hierarchy-panel">
  <!-- Moodle Role Hierarchy Display Section -->
  <div class="border-b border-outline-variant pb-3">
    <div class="flex items-center justify-between mb-2">
      <h5 class="text-sm font-semibold text-on-surface flex items-center gap-1.5">
        <span>Иерархия маппинга ролей Moodle</span>
      </h5>
      <button
        type="button"
        on:click={fetchRoleHierarchy}
        class="text-xs text-primary hover:underline focus:outline-none"
        title="Обновить иерархию ролей"
      >
        Обновить
      </button>
    </div>
    <p class="text-xs text-on-surface-variant mb-3">
      Сопоставление внешних ролей системы Moodle с внутренними ролями архива института:
    </p>

    {#if hierarchyLoading}
      <div class="py-2 text-xs text-on-surface-variant flex items-center gap-2" id="hierarchy-loading-state">
        <span class="inline-block animate-spin rounded-full h-3.5 w-3.5 border-2 border-primary border-t-transparent"></span>
        Загрузка иерархии ролей...
      </div>
    {:else if hierarchyError}
      <div class="p-2 rounded bg-error-container text-on-error-container text-xs" role="alert" id="hierarchy-error-state">
        {hierarchyError}
      </div>
    {:else}
      <div class="overflow-x-auto" id="hierarchy-mapping-list">
        <table class="w-full text-left text-xs border-collapse">
          <thead>
            <tr class="border-b border-outline-variant bg-surface-container text-on-surface-variant">
              <th class="py-1.5 px-2 font-medium">Роль / Паттерн в Moodle</th>
              <th class="py-1.5 px-2 font-medium">Внутренняя роль в системе</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/40">
            {#each roleMappings as mapping}
              <tr class="hover:bg-surface-container-lowest/50">
                <td class="py-1.5 px-2 font-mono text-on-surface">{mapping.moodle_role_pattern}</td>
                <td class="py-1.5 px-2 font-semibold text-primary">
                  {getRoleTitle(mapping.internal_role || mapping.internalRole)}
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {/if}
  </div>

  <!-- Role Override Form -->
  <div>
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
</div>
