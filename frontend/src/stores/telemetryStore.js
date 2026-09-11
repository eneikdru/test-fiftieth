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

export function createTelemetryStore() {
  const initialState = {
    totalClicks: 0,
    tabNavigations: 0,
    successfulLoads: 1,
    failedLoads: 0,
    activities: [
      { id: 1, title: 'Инициализация приложения', module: 'Единая консоль', latencyMs: 18, timestamp: 'Только что', success: true }
    ],
    preConsolidationTaps: 3.2
  };

  const store = writable(initialState);
  const { subscribe, set, update } = store;

  return {
    subscribe,
    set,
    update,
    reset: () => set(initialState),

    recordClick() {
      update(s => ({
        ...s,
        totalClicks: s.totalClicks + 1
      }));
    },

    recordTabNavigation(tabKey, tabTitle) {
      update(s => {
        const nextActivityId = Date.now();
        const latency = 12 + (s.activities.length % 8) * 3;
        const newActivities = [
          {
            id: nextActivityId,
            title: `Переход: ${tabTitle || tabKey}`,
            module: `${tabKey.toUpperCase()} Module`,
            latencyMs: latency,
            timestamp: 'Только что',
            success: true
          },
          ...s.activities
        ].slice(0, 15);

        return {
          ...s,
          totalClicks: s.totalClicks + 1,
          tabNavigations: s.tabNavigations + 1,
          successfulLoads: s.successfulLoads + 1,
          activities: newActivities
        };
      });
    }
  };
}

export const telemetryStore = createTelemetryStore();
