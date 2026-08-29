<script>
  import { createEventDispatcher } from 'svelte';
  import ImprintModal from './ImprintModal.svelte';

  const dispatch = createEventDispatcher();

  // Props
  export function getApiBaseUrl() {
    return '/api/v1';
  }

  // State
  let mode = 'login'; // 'login' | 'recovery' | 'recovery_sent' | 'reset_password' | 'recovery_completed' | 'authenticated'
  let username = '';
  let password = '';
  let recoveryIdentity = '';
  let recoveryToken = '';
  let newPassword = '';
  let isLoading = false;
  let errorMessage = '';
  let successMessage = '';
  let currentUser = null; // { id, username, role, full_name, email }
  let showImprint = false;

  // Admin status helper
  $: isAdmin = currentUser && currentUser.role === 'ADMIN';

  async function handleLogin(event) {
    event.preventDefault();
    errorMessage = '';
    successMessage = '';

    if (!username.trim() || !password) {
      errorMessage = 'Заполните все обязательные поля.';
      return;
    }

    isLoading = true;
    try {
      const response = await fetch(`${getApiBaseUrl()}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: username.trim(),
          password: password
        })
      });

      if (!response.ok) {
        let errData = {};
        try {
          errData = await response.json();
        } catch (e) {}
        if (response.status === 401) {
          throw new Error(errData.message || 'Неверное имя пользователя или пароль.');
        } else if (response.status === 423) {
          throw new Error(errData.message || 'Учетная запись заблокирована из-за соображений безопасности.');
        } else {
          throw new Error(errData.message || 'Ошибка аутентификации. Попробуйте позже.');
        }
      }

      const data = await response.json();
      currentUser = data.user || {
        id: 101,
        username: username.trim(),
        role: 'RESEARCHER',
        full_name: 'Сотрудник института',
        email: username.includes('@') ? username.trim() : `${username.trim()}@epidemiology-inst.ru`
      };

      if (data.access_token) {
        localStorage.setItem('access_token', data.access_token);
      }
      if (data.refresh_token) {
        localStorage.setItem('refresh_token', data.refresh_token);
      }

      mode = 'authenticated';
      dispatch('loginSuccess', { user: currentUser });
    } catch (err) {
      errorMessage = err.message || 'Произошла ошибка при входе в систему.';
    } finally {
      isLoading = false;
    }
  }

  async function handleRecoveryRequest(event) {
    event.preventDefault();
    errorMessage = '';
    successMessage = '';

    if (!recoveryIdentity.trim()) {
      errorMessage = 'Пожалуйста, укажите ваш email или имя пользователя.';
      return;
    }

    isLoading = true;
    try {
      const response = await fetch(`${getApiBaseUrl()}/auth/recovery/request`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          identity: recoveryIdentity.trim()
        })
      });

      if (!response.ok) {
        let errData = {};
        try {
          errData = await response.json();
        } catch (e) {}
        if (response.status === 404) {
          throw new Error(errData.message || 'Учетная запись с указанными данными не найдена.');
        } else if (response.status === 429) {
          throw new Error(errData.message || 'Слишком много попыток восстановления. Попробуйте позже.');
        } else {
          throw new Error(errData.message || 'Не удалось отправить запрос на восстановление.');
        }
      }

      const data = await response.json();
      successMessage = data.message || 'Инструкции по восстановлению пароля отправлены на ваш электронный адрес.';
      recoveryToken = data.recovery_token || '';
      mode = 'recovery_sent';
    } catch (err) {
      errorMessage = err.message || 'Произошла ошибка при запросе восстановления пароля.';
    } finally {
      isLoading = false;
    }
  }

  async function handleConfirmReset(event) {
    event.preventDefault();
    errorMessage = '';
    successMessage = '';

    if (!recoveryToken.trim() || !newPassword) {
      errorMessage = 'Заполните токен восстановления и новый пароль.';
      return;
    }

    isLoading = true;
    try {
      const response = await fetch(`${getApiBaseUrl()}/auth/recovery/reset`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          recovery_token: recoveryToken.trim(),
          new_password: newPassword
        })
      });

      if (!response.ok) {
        let errData = {};
        try {
          errData = await response.json();
        } catch (e) {}
        throw new Error(errData.message || 'Не удалось сбросить пароль.');
      }

      const data = await response.json();
      successMessage = data.message || 'Пароль успешно изменен.';
      password = newPassword;
      mode = 'recovery_completed';
    } catch (err) {
      errorMessage = err.message || 'Произошла ошибка при сбросе пароля.';
    } finally {
      isLoading = false;
    }
  }

  async function handleLogout() {
    isLoading = true;
    const refreshToken = localStorage.getItem('refresh_token') || '';
    try {
      if (refreshToken) {
        await fetch(`${getApiBaseUrl()}/auth/logout`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('access_token') || ''}`
          },
          body: JSON.stringify({ refresh_token: refreshToken })
        });
      }
    } catch (e) {
      // Ignore network errors on logout
    } finally {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      currentUser = null;
      username = '';
      password = '';
      recoveryIdentity = '';
      errorMessage = '';
      successMessage = '';
      mode = 'login';
      isLoading = false;
      dispatch('logout');
    }
  }

  function switchToRecovery() {
    errorMessage = '';
    successMessage = '';
    mode = 'recovery';
  }

  function switchToLogin() {
    errorMessage = '';
    successMessage = '';
    mode = 'login';
  }
</script>

<div class="auth-wrapper font-sans min-h-screen bg-[#f9f9fc] text-[#1a1c1e] flex flex-col justify-between antialiased">
  <!-- Top App Bar -->
  <header class="w-full top-0 sticky bg-[#f9f9fc] flex items-center justify-between h-16 px-6 max-w-md mx-auto z-10 border-b border-[#e2e2e5]">
    {#if mode === 'recovery' || mode === 'recovery_sent'}
      <button
        type="button"
        aria-label="Назад к входу"
        on:click={switchToLogin}
        class="mr-4 text-[#00328a] hover:opacity-80 transition-opacity flex items-center justify-center p-2 rounded-full focus:outline-none focus:ring-2 focus:ring-[#00328a]"
      >
        <span class="material-symbols-outlined text-2xl" data-icon="arrow_back">←</span>
      </button>
      <h1 class="font-semibold text-lg text-[#00328a] flex-1">Восстановление</h1>
    {:else}
      <div class="flex items-center space-x-2">
        <span class="text-[#00328a] font-bold text-xl tracking-tight">ЭпидБаза</span>
      </div>
      <span class="text-xs text-[#434653] font-medium bg-[#eeeef0] px-2.5 py-1 rounded-full">НИИ Эпидемиологии</span>
    {/if}
  </header>

  <!-- Main Canvas -->
  <main class="flex-1 flex flex-col justify-center px-6 max-w-md mx-auto w-full pt-6 pb-8">
    {#if mode === 'login'}
      <!-- LOGIN VIEW -->
      <div class="mb-8">
        <h2 class="text-3xl font-bold text-[#1a1c1e] mb-2 tracking-tight">Вход в систему</h2>
        <p class="text-base text-[#434653]">
          База знаний по эпидемиологии. Введите учетные данные института для доступа.
        </p>
      </div>

      {#if errorMessage}
        <div role="alert" class="mb-6 p-4 rounded-lg bg-[#ffdad6] text-[#93000a] text-sm border border-[#ba1a1a]/20 flex items-start space-x-2">
          <span class="font-bold">!</span>
          <span>{errorMessage}</span>
        </div>
      {/if}

      <form on:submit={handleLogin} class="flex flex-col space-y-5" novalidate>
        <div>
          <label for="username-input" class="block text-sm font-semibold text-[#1a1c1e] mb-2">
            Имя пользователя или Email
          </label>
          <div class="relative rounded-md border border-[#c3c6d6] bg-white focus-within:border-[#00328a] focus-within:ring-2 focus-within:ring-[#00328a]/10 transition-all">
            <input
              id="username-input"
              type="text"
              bind:value={username}
              placeholder="ivanov@epidemiology-inst.ru"
              required
          aria-label="Имя пользователя или электронная почта"
              disabled={isLoading}
              class="w-full h-14 px-4 bg-transparent border-none rounded-md focus:outline-none text-base text-[#1a1c1e] placeholder-[#737685]"
            />
          </div>
        </div>

        <div>
          <div class="flex justify-between items-center mb-2">
            <label for="password-input" class="block text-sm font-semibold text-[#1a1c1e]">
              Пароль
            </label>
            <button
              type="button"
              on:click={switchToRecovery}
              class="text-sm font-medium text-[#00328a] hover:underline focus:outline-none focus:ring-1 focus:ring-[#00328a] rounded px-1"
            >
              Забыли пароль?
            </button>
          </div>
          <div class="relative rounded-md border border-[#c3c6d6] bg-white focus-within:border-[#00328a] focus-within:ring-2 focus-within:ring-[#00328a]/10 transition-all">
            <input
              id="password-input"
              type="password"
              bind:value={password}
              placeholder="••••••••"
              required
          aria-label="Пароль пользователя"
              disabled={isLoading}
              class="w-full h-14 px-4 bg-transparent border-none rounded-md focus:outline-none text-base text-[#1a1c1e] placeholder-[#737685]"
            />
          </div>
        </div>

        <div class="pt-4">
          <button
            type="submit"
            disabled={isLoading}
            class="w-full h-13 bg-[#00328a] text-white rounded-lg font-medium text-base py-3 flex items-center justify-center hover:bg-[#002566] active:scale-[0.98] transition-all disabled:opacity-60 disabled:cursor-not-allowed shadow-sm"
          >
            {#if isLoading}
              <svg class="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" fill="currentColor"></path>
              </svg>
            {:else}
              <span>Войти</span>
            {/if}
          </button>
        </div>
      </form>

    {:else if mode === 'recovery'}
      <!-- RECOVERY REQUEST VIEW -->
      <div class="mb-8">
        <h2 class="text-3xl font-bold text-[#1a1c1e] mb-2 tracking-tight">Забыли пароль?</h2>
        <p class="text-base text-[#434653]">
          Введите ваш email или логин, чтобы получить инструкции по сбросу пароля.
        </p>
      </div>

      {#if errorMessage}
        <div role="alert" class="mb-6 p-4 rounded-lg bg-[#ffdad6] text-[#93000a] text-sm border border-[#ba1a1a]/20 flex items-start space-x-2">
          <span class="font-bold">!</span>
          <span>{errorMessage}</span>
        </div>
      {/if}

      <form on:submit={handleRecoveryRequest} class="flex flex-col space-y-5" novalidate>
        <div>
          <label for="recovery-identity-input" class="block text-sm font-semibold text-[#1a1c1e] mb-2">
            Email или логин
          </label>
          <div class="relative rounded-md border border-[#c3c6d6] bg-white focus-within:border-[#00328a] focus-within:ring-2 focus-within:ring-[#00328a]/10 transition-all">
            <input
              id="recovery-identity-input"
              type="text"
              bind:value={recoveryIdentity}
              placeholder="name@example.com"
              required
              disabled={isLoading}
              class="w-full h-14 px-4 bg-transparent border-none rounded-md focus:outline-none text-base text-[#1a1c1e] placeholder-[#737685]"
            />
          </div>
        </div>

        <div class="pt-4">
          <button
            type="submit"
            disabled={isLoading}
            class="w-full h-13 bg-[#00328a] text-white rounded-lg font-medium text-base py-3 flex items-center justify-center hover:bg-[#002566] active:scale-[0.98] transition-all disabled:opacity-60 disabled:cursor-not-allowed shadow-sm"
          >
            {#if isLoading}
              <svg class="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" fill="currentColor"></path>
              </svg>
            {:else}
              <span>Отправить</span>
            {/if}
          </button>
        </div>
      </form>

    {:else if mode === 'recovery_sent'}
      <!-- RECOVERY SENT CONFIRMATION VIEW -->
      <div class="p-6 bg-white rounded-xl border border-[#c3c6d6] space-y-4 shadow-sm">
        <div class="w-12 h-12 bg-[#00328a]/10 text-[#00328a] rounded-full flex items-center justify-center mx-auto text-2xl font-bold">
          ✓
        </div>
        <h3 class="text-xl font-bold text-[#1a1c1e] text-center">Инструкции отправлены</h3>
        <p class="text-sm text-[#434653] leading-relaxed text-center">
          {successMessage}
        </p>

        <form on:submit={handleConfirmReset} class="flex flex-col space-y-4 pt-3 border-t border-[#eeeef0]" novalidate>
          <div>
            <label for="recovery-token-input" class="block text-sm font-semibold text-[#1a1c1e] mb-1">
              Токен восстановления
            </label>
            <input
              id="recovery-token-input"
              type="text"
              bind:value={recoveryToken}
              required
              aria-label="Токен восстановления пароля"
              class="w-full h-11 px-3 border border-[#c3c6d6] rounded-md text-sm text-[#1a1c1e]"
            />
          </div>
          <div>
            <label for="new-password-input" class="block text-sm font-semibold text-[#1a1c1e] mb-1">
              Новый пароль
            </label>
            <input
              id="new-password-input"
              type="password"
              bind:value={newPassword}
              required
              aria-label="Новый пароль"
              class="w-full h-11 px-3 border border-[#c3c6d6] rounded-md text-sm text-[#1a1c1e]"
            />
          </div>
          <button
            type="submit"
            id="confirm-reset-submit-btn"
            disabled={isLoading}
            class="w-full py-3 bg-[#00328a] text-white rounded-lg text-sm font-medium hover:bg-[#002566] transition-colors"
          >
            Сбросить пароль и восстановить доступ
          </button>
        </form>

        <button
          type="button"
          on:click={switchToLogin}
          class="w-full py-2 border border-[#c3c6d6] text-[#434653] rounded-lg text-sm font-medium hover:bg-[#eeeef0] transition-colors"
        >
          Вернуться ко входу
        </button>
      </div>

    {:else if mode === 'recovery_completed'}
      <!-- RECOVERY COMPLETED VIEW -->
      <div class="p-6 bg-white rounded-xl border border-[#c3c6d6] text-center space-y-4 shadow-sm">
        <div class="w-12 h-12 bg-[#00328a]/10 text-[#00328a] rounded-full flex items-center justify-center mx-auto text-2xl font-bold">
          ✓
        </div>
        <h3 class="text-xl font-bold text-[#1a1c1e]">Доступ успешно восстановлен</h3>
        <p id="recovery-completed-message" class="text-sm text-[#434653] leading-relaxed">
          {successMessage || 'Ваш пароль успешно изменен.'}
        </p>
        <button
          type="button"
          on:click={switchToLogin}
          class="w-full py-3 bg-[#00328a] text-white rounded-lg text-sm font-medium hover:bg-[#002566] transition-colors"
        >
          Перейти ко входу
        </button>
      </div>

    {:else if mode === 'authenticated'}
      <!-- AUTHENTICATED USER VIEW -->
      <div class="bg-white p-6 rounded-xl border border-[#c3c6d6] shadow-sm space-y-6">
        <div class="flex items-center space-x-3 pb-4 border-b border-[#eeeef0]">
          <div class="w-10 h-10 rounded-full bg-[#00328a] text-white flex items-center justify-center font-bold text-lg">
            {(currentUser.full_name || currentUser.username || 'С')[0]}
          </div>
          <div>
            <h3 class="font-bold text-[#1a1c1e] text-base">{currentUser.full_name || currentUser.username}</h3>
            <p class="text-xs text-[#434653]">
              Роль: <span class="font-semibold">{currentUser.role || 'RESEARCHER'}</span>
            </p>
          </div>
        </div>

        <div class="space-y-3">
          <h4 class="text-sm font-semibold text-[#1a1c1e]">Каталог эпидемиологических материалов</h4>
          <p class="text-xs text-[#434653]">
            Вам доступен просмотр и поиск протоколов расследований вспышек, отчетов эпиднадзора и методических руководств.
          </p>

          <!-- Explicit constraint: Upload and delete controls MUST be entirely hidden for non-admin employees -->
          {#if isAdmin}
            <div class="p-3 bg-[#f3f3f6] rounded border border-[#c3c6d6] flex space-x-2">
              <button class="px-3 py-1.5 bg-[#00328a] text-white text-xs rounded font-medium">Загрузить документ</button>
              <button class="px-3 py-1.5 bg-[#ba1a1a] text-white text-xs rounded font-medium">Удалить</button>
            </div>
          {/if}
        </div>

        <div class="pt-2">
          <button
            type="button"
            on:click={handleLogout}
            disabled={isLoading}
            class="w-full py-2.5 border border-[#c3c6d6] text-[#1a1c1e] hover:bg-[#eeeef0] rounded-lg text-sm font-medium transition-colors"
          >
            Выйти из системы
          </button>
        </div>
      </div>
    {/if}
  </main>

  {#if showImprint}
    <ImprintModal on:close={() => showImprint = false} />
  {/if}

  <!-- Footer -->
  <footer class="w-full py-4 text-center text-xs text-[#737685] border-t border-[#eeeef0] flex flex-col sm:flex-row items-center justify-between gap-2 px-6">
    <p>Российский научно-исследовательский институт эпидемиологии</p>
    <div>
      <button
        type="button"
        id="imprint-link-login"
        on:click={() => showImprint = true}
        class="text-[#00328a] hover:underline focus:outline-none focus:ring-2 focus:ring-[#00328a]/50 rounded px-1 font-semibold"
      >
        Выходные данные (Imprint / Impressum)
      </button>
    </div>
  </footer>
</div>
