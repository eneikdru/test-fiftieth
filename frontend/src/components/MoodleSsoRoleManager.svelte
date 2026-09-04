<script>
  // Props
  export let mode = 'login'; // 'login' | 'profile'
  export let isAdmin = false;
  export let currentMoodleRole = 'RESEARCHER';

  // State for Admin Override
  let isEditingRole = false;
  let selectedOverrideRole = currentMoodleRole;

  // Constants based on brief
  const availableRoles = [
    { value: 'ADMIN', label: 'Администратор' },
    { value: 'SENIOR_RESEARCHER', label: 'Старший научный сотрудник / Эпидемиолог (право подписи)' },
    { value: 'RESEARCHER', label: 'Исследователь / Аспирант' }
  ];

  function handleMoodleSsoLogin() {
    // Initiate SSO
    console.log('Initiating Moodle SSO...');
    window.location.href = '/api/v1/auth/moodle/config';
  }

  function handleSaveOverride() {
    // Save override
    console.log('Saving role override:', selectedOverrideRole);
    currentMoodleRole = selectedOverrideRole;
    isEditingRole = false;
  }
</script>

<div class="moodle-sso-container w-full max-w-md mx-auto">
  {#if mode === 'login'}
    <div class="flex flex-col gap-4">
      <button
        type="button"
        on:click={handleMoodleSsoLogin}
        class="w-full h-[48px] bg-[#00328a] text-white rounded-lg flex items-center justify-center gap-2 hover:bg-[#002566] active:scale-95 transition-all focus:outline-none focus:ring-4 focus:ring-[#00328a]/50 shadow-sm font-semibold text-base"
        aria-label="Login via Moodle"
      >
        <span class="material-symbols-outlined font-bold" aria-hidden="true">school</span>
        <span>Login via Moodle</span>
      </button>

      <!-- Button meets WCAG 2.1 AA with white text on #00328a -->
    </div>
  {:else if mode === 'profile' && isAdmin}
    <div class="bg-white p-6 rounded-xl border border-[#c3c6d6] shadow-sm space-y-4">
      <h3 class="text-lg font-bold text-[#1a1c1e] flex items-center gap-2">
        <span class="material-symbols-outlined text-[#00328a]">manage_accounts</span>
        Управление ролями Moodle (Override)
      </h3>

      <div class="text-sm text-[#434653]">
        <p class="mb-2">Текущая синхронизированная роль:</p>
        <div class="font-medium text-[#1a1c1e] bg-[#f7f9fb] p-2 rounded border border-[#e0e3e5]">
          {availableRoles.find(r => r.value === currentMoodleRole)?.label || currentMoodleRole}
        </div>
      </div>

      {#if isEditingRole}
        <div class="space-y-3 pt-2">
          <label class="block text-xs font-semibold text-[#1a1c1e]" for="role-override-select">
            Назначить переопределение роли
          </label>
          <div class="relative">
            <select
              id="role-override-select"
              bind:value={selectedOverrideRole}
              class="w-full h-10 px-3 bg-white border border-[#c3c6d6] rounded-md text-sm text-[#1a1c1e] focus:outline-none focus:ring-2 focus:ring-[#00328a]/50 focus:border-[#00328a]"
              aria-label="Выбор новой роли"
            >
              {#each availableRoles as role}
                <option value={role.value}>{role.label}</option>
              {/each}
            </select>
          </div>

          <div class="flex gap-2 pt-2">
            <button
              on:click={handleSaveOverride}
              class="px-4 py-2 bg-[#00328a] text-white text-sm font-medium rounded-lg hover:bg-[#002566] transition-colors h-[48px]"
            >
              Сохранить
            </button>
            <button
              on:click={() => { isEditingRole = false; selectedOverrideRole = currentMoodleRole; }}
              class="px-4 py-2 bg-transparent text-[#434653] text-sm font-medium rounded-lg border border-[#c3c6d6] hover:bg-[#f7f9fb] transition-colors h-[48px]"
            >
              Отмена
            </button>
          </div>
        </div>
      {:else}
        <div class="pt-2">
          <button
            on:click={() => isEditingRole = true}
            class="px-4 py-2 bg-[#f3f3f6] text-[#00328a] text-sm font-medium rounded-lg hover:bg-[#e6e6e9] transition-colors flex items-center justify-center gap-1 border border-[#c3c6d6] h-[48px] w-full sm:w-auto"
          >
            <span class="material-symbols-outlined text-sm">edit</span>
            Переопределить роль
          </button>
        </div>
      {/if}
    </div>
  {/if}
</div>
