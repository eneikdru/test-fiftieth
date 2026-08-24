<script>
  // Modes: 'landing', 'registration', 'onboarding_1', 'onboarding_2'
  export let mode = 'landing';

  let isLoading = false;
  let errorMessage = '';

  // Registration fields
  let username = '';
  let email = '';
  let password = '';
  let confirmPassword = '';

  // Onboarding fields
  let firstName = '';
  let lastName = '';
  let organization = '';

  function handleRegistration(e) {
    e.preventDefault();
    errorMessage = '';

    if (!username || !email || !password || !confirmPassword) {
      errorMessage = 'Пожалуйста, заполните все обязательные поля.';
      return;
    }

    if (password !== confirmPassword) {
      errorMessage = 'Пароли не совпадают.';
      return;
    }

    isLoading = true;
    setTimeout(() => {
      isLoading = false;
      mode = 'onboarding_1';
    }, 400);
  }

  function handleOnboarding1(e) {
    if (e) e.preventDefault();
    errorMessage = '';

    if (!firstName || !lastName) {
      errorMessage = 'Имя и фамилия обязательны для заполнения.';
      return;
    }

    isLoading = true;
    setTimeout(() => {
      isLoading = false;
      mode = 'onboarding_2';
    }, 400);
  }

  function skipOnboarding1() {
    mode = 'onboarding_2';
  }

  function finishSetup() {
    isLoading = true;
    setTimeout(() => {
      isLoading = false;
      // Would normally redirect to main app
      alert('Настройка завершена!');
    }, 400);
  }
</script>

<div class="min-h-screen bg-[#f7f9fb] flex flex-col relative font-sans">
  <header class="w-full flex items-center justify-between h-16 px-6 z-10 bg-white border-b border-[#e2e2e5]">
    <div class="flex items-center space-x-2">
      <span class="text-[#00328a] font-bold text-xl tracking-tight">ЭпидБаза</span>
      <span class="text-xs text-[#434653] font-medium bg-[#eeeef0] px-2.5 py-1 rounded-full hidden sm:inline-block">НИИ Эпидемиологии</span>
    </div>

    {#if mode === 'landing'}
      <button
        type="button"
        on:click={() => mode = 'registration'}
        class="text-sm font-semibold text-[#00328a] hover:underline focus:outline-none focus:ring-2 focus:ring-[#00328a] rounded-md px-3 py-1.5 transition-all"
      >
        Регистрация
      </button>
    {:else if mode === 'registration'}
      <button
        type="button"
        on:click={() => mode = 'landing'}
        class="text-sm font-medium text-[#434653] hover:text-[#00328a] focus:outline-none focus:ring-2 focus:ring-[#00328a] rounded-md px-3 py-1.5 transition-all"
      >
        К входу
      </button>
    {/if}
  </header>

  <main class="flex-1 flex flex-col justify-center items-center px-4 py-8 w-full">
    {#if mode === 'landing'}
      <div class="max-w-3xl w-full text-center space-y-8 animate-fade-in">
        <h1 class="text-4xl sm:text-5xl font-extrabold text-[#1a1c1e] tracking-tight">
          База знаний по эпидемиологии
        </h1>
        <p class="text-lg text-[#434653] max-w-xl mx-auto">
          Единая система для каталогизации, поиска и управления эпидемиологическими материалами, протоколами и данными института.
        </p>
        <div class="pt-4 flex flex-col sm:flex-row items-center justify-center space-y-4 sm:space-y-0 sm:space-x-4">
          <button
            on:click={() => mode = 'registration'}
            class="w-full sm:w-auto px-8 py-3.5 bg-[#00328a] text-white rounded-lg font-medium text-base hover:bg-[#002566] active:scale-[0.98] transition-all shadow-sm focus:outline-none focus:ring-4 focus:ring-[#00328a]/20"
          >
            Создать аккаунт
          </button>
          <button
            class="w-full sm:w-auto px-8 py-3.5 bg-white border border-[#c3c6d6] text-[#1a1c1e] rounded-lg font-medium text-base hover:bg-[#f3f3f6] active:scale-[0.98] transition-all focus:outline-none focus:ring-4 focus:ring-[#c3c6d6]/50"
          >
            Войти в систему
          </button>
        </div>
      </div>

    {:else if mode === 'registration'}
      <div class="w-full max-w-md bg-white p-8 rounded-xl shadow-sm border border-[#e2e2e5]">
        <h2 class="text-2xl font-bold text-[#1a1c1e] mb-6 tracking-tight text-center">Создать аккаунт</h2>

        {#if errorMessage}
          <div role="alert" class="mb-6 p-4 rounded-lg bg-[#ffdad6] text-[#93000a] text-sm border border-[#ba1a1a]/20 flex items-start space-x-2">
            <span class="font-bold material-symbols-outlined text-base">error</span>
            <span>{errorMessage}</span>
          </div>
        {/if}

        <form on:submit={handleRegistration} class="space-y-5" novalidate>
          <div>
            <label for="username" class="block text-sm font-semibold text-[#1a1c1e] mb-1.5">Имя пользователя</label>
            <input
              id="username"
              type="text"
              bind:value={username}
              disabled={isLoading}
              required
              class="w-full h-12 px-4 rounded-md border border-[#c3c6d6] focus:border-[#00328a] focus:ring-2 focus:ring-[#00328a]/10 outline-none transition-all text-[#1a1c1e] text-base"
            />
          </div>

          <div>
            <label for="email" class="block text-sm font-semibold text-[#1a1c1e] mb-1.5">Электронная почта</label>
            <input
              id="email"
              type="email"
              bind:value={email}
              disabled={isLoading}
              required
              class="w-full h-12 px-4 rounded-md border border-[#c3c6d6] focus:border-[#00328a] focus:ring-2 focus:ring-[#00328a]/10 outline-none transition-all text-[#1a1c1e] text-base"
            />
          </div>

          <div>
            <label for="password" class="block text-sm font-semibold text-[#1a1c1e] mb-1.5">Пароль</label>
            <input
              id="password"
              type="password"
              bind:value={password}
              disabled={isLoading}
              required
              class="w-full h-12 px-4 rounded-md border border-[#c3c6d6] focus:border-[#00328a] focus:ring-2 focus:ring-[#00328a]/10 outline-none transition-all text-[#1a1c1e] text-base"
            />
          </div>

          <div>
            <label for="confirm-password" class="block text-sm font-semibold text-[#1a1c1e] mb-1.5">Подтвердите пароль</label>
            <input
              id="confirm-password"
              type="password"
              bind:value={confirmPassword}
              disabled={isLoading}
              required
              class="w-full h-12 px-4 rounded-md border border-[#c3c6d6] focus:border-[#00328a] focus:ring-2 focus:ring-[#00328a]/10 outline-none transition-all text-[#1a1c1e] text-base"
            />
          </div>

          <div class="pt-2">
            <button
              type="submit"
              disabled={isLoading}
              class="w-full h-12 bg-[#00328a] text-white rounded-lg font-medium text-base hover:bg-[#002566] active:scale-[0.98] transition-all disabled:opacity-60 disabled:cursor-not-allowed shadow-sm flex items-center justify-center"
            >
              {#if isLoading}
                <svg class="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" fill="currentColor"></path>
                </svg>
              {:else}
                Продолжить
              {/if}
            </button>
          </div>
        </form>
      </div>

    {:else if mode === 'onboarding_1'}
      <div class="w-full max-w-md bg-white p-8 rounded-xl shadow-sm border border-[#e2e2e5]">
        <div class="flex justify-center space-x-2 mb-6">
          <div class="w-2.5 h-2.5 rounded-full bg-[#00328a]"></div>
          <div class="w-2.5 h-2.5 rounded-full border border-[#c3c6d6]"></div>
          <div class="w-2.5 h-2.5 rounded-full border border-[#c3c6d6]"></div>
        </div>

        <h2 class="text-2xl font-bold text-[#1a1c1e] mb-2 tracking-tight text-center">Ваш профиль</h2>
        <p class="text-sm text-[#434653] text-center mb-6">
          Расскажите немного о себе, чтобы коллеги могли вас найти.
        </p>

        {#if errorMessage}
          <div role="alert" class="mb-6 p-4 rounded-lg bg-[#ffdad6] text-[#93000a] text-sm border border-[#ba1a1a]/20 flex items-start space-x-2">
            <span class="font-bold material-symbols-outlined text-base">error</span>
            <span>{errorMessage}</span>
          </div>
        {/if}

        <form on:submit={handleOnboarding1} class="space-y-5" novalidate>
          <div>
            <label for="firstName" class="block text-sm font-semibold text-[#1a1c1e] mb-1.5">Имя</label>
            <input
              id="firstName"
              type="text"
              bind:value={firstName}
              disabled={isLoading}
              required
              class="w-full h-12 px-4 rounded-md border border-[#c3c6d6] focus:border-[#00328a] focus:ring-2 focus:ring-[#00328a]/10 outline-none transition-all text-[#1a1c1e] text-base"
            />
          </div>

          <div>
            <label for="lastName" class="block text-sm font-semibold text-[#1a1c1e] mb-1.5">Фамилия</label>
            <input
              id="lastName"
              type="text"
              bind:value={lastName}
              disabled={isLoading}
              required
              class="w-full h-12 px-4 rounded-md border border-[#c3c6d6] focus:border-[#00328a] focus:ring-2 focus:ring-[#00328a]/10 outline-none transition-all text-[#1a1c1e] text-base"
            />
          </div>

          <div>
            <label for="organization" class="block text-sm font-semibold text-[#1a1c1e] mb-1.5">Организация (необязательно)</label>
            <input
              id="organization"
              type="text"
              bind:value={organization}
              disabled={isLoading}
              class="w-full h-12 px-4 rounded-md border border-[#c3c6d6] focus:border-[#00328a] focus:ring-2 focus:ring-[#00328a]/10 outline-none transition-all text-[#1a1c1e] text-base"
            />
          </div>

          <div class="pt-4 flex items-center justify-between">
            <button
              type="button"
              on:click={skipOnboarding1}
              disabled={isLoading}
              class="text-sm font-medium text-[#434653] hover:text-[#1a1c1e] px-2 py-1 rounded"
            >
              Пропустить
            </button>
            <button
              type="submit"
              disabled={isLoading}
              class="px-6 h-11 bg-[#00328a] text-white rounded-lg font-medium text-sm hover:bg-[#002566] active:scale-[0.98] transition-all disabled:opacity-60 disabled:cursor-not-allowed shadow-sm flex items-center justify-center"
            >
              {#if isLoading}
                <svg class="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" fill="currentColor"></path>
                </svg>
              {:else}
                Далее
              {/if}
            </button>
          </div>
        </form>
      </div>

    {:else if mode === 'onboarding_2'}
      <div class="w-full max-w-2xl bg-white p-8 rounded-xl shadow-sm border border-[#e2e2e5]">
        <div class="flex justify-center space-x-2 mb-6">
          <div class="w-2.5 h-2.5 rounded-full border border-[#c3c6d6]"></div>
          <div class="w-2.5 h-2.5 rounded-full bg-[#00328a]"></div>
          <div class="w-2.5 h-2.5 rounded-full border border-[#c3c6d6]"></div>
        </div>

        <h2 class="text-2xl font-bold text-[#1a1c1e] mb-2 tracking-tight text-center">Настройка рабочего пространства</h2>
        <p class="text-sm text-[#434653] text-center mb-8">
          Быстрые действия для начала работы с базой знаний
        </p>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
          <div class="border border-[#c3c6d6] rounded-lg p-5 flex flex-col items-center text-center hover:border-[#00328a] hover:bg-[#00328a]/5 transition-colors cursor-pointer group">
            <div class="w-12 h-12 bg-[#eeeef0] group-hover:bg-white rounded-full flex items-center justify-center mb-3">
              <span class="material-symbols-outlined text-[#00328a] text-2xl">group_add</span>
            </div>
            <h3 class="font-semibold text-[#1a1c1e] text-sm mb-1">Пригласить коллег</h3>
            <p class="text-xs text-[#737685]">Добавьте сотрудников в систему</p>
          </div>

          <div class="border border-[#c3c6d6] rounded-lg p-5 flex flex-col items-center text-center hover:border-[#00328a] hover:bg-[#00328a]/5 transition-colors cursor-pointer group">
            <div class="w-12 h-12 bg-[#eeeef0] group-hover:bg-white rounded-full flex items-center justify-center mb-3">
              <span class="material-symbols-outlined text-[#00328a] text-2xl">create_new_folder</span>
            </div>
            <h3 class="font-semibold text-[#1a1c1e] text-sm mb-1">Создать проект</h3>
            <p class="text-xs text-[#737685]">Организуйте документы</p>
          </div>

          <div class="border border-[#c3c6d6] rounded-lg p-5 flex flex-col items-center text-center hover:border-[#00328a] hover:bg-[#00328a]/5 transition-colors cursor-pointer group">
            <div class="w-12 h-12 bg-[#eeeef0] group-hover:bg-white rounded-full flex items-center justify-center mb-3">
              <span class="material-symbols-outlined text-[#00328a] text-2xl">upload_file</span>
            </div>
            <h3 class="font-semibold text-[#1a1c1e] text-sm mb-1">Загрузить файлы</h3>
            <p class="text-xs text-[#737685]">Добавьте первые протоколы</p>
          </div>
        </div>

        <div class="flex justify-center">
          <button
            type="button"
            on:click={finishSetup}
            disabled={isLoading}
            class="px-8 h-12 bg-[#00328a] text-white rounded-lg font-medium text-base hover:bg-[#002566] active:scale-[0.98] transition-all disabled:opacity-60 disabled:cursor-not-allowed shadow-sm flex items-center justify-center"
          >
            {#if isLoading}
              <svg class="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" fill="currentColor"></path>
              </svg>
            {:else}
              Завершить настройку
            {/if}
          </button>
        </div>
      </div>
    {/if}
  </main>
</div>

<style>
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
  }
  .animate-fade-in {
    animation: fadeIn 0.4s ease-out forwards;
  }
</style>
