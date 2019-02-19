import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

#create multindex dataframe
arrays = [['Fruit', 'Fruit', 'Fruit', 'Veggies', 'Veggies', 'Veggies'],
          ['Bananas', 'Oranges', 'Pears', 'Carrots', 'Potatoes', 'Celery']]
index = pd.MultiIndex.from_tuples(list(zip(*arrays)))
df = pd.DataFrame(np.random.randint(10, 50, size=(1, 6)), columns=index)

#plotting
fig, axes = plt.subplots(nrows=1, ncols=2, sharey=True, figsize=(14 / 2.54, 10 / 2.54))  # width, height
for i, col in enumerate(df.columns.levels[0]):
    print(col)
    ax = axes[i]
    df[col].T.plot(ax=ax, kind='bar', width=.8)

    ax.legend_.remove()
    ax.set_xlabel(col, weight='bold')
    ax.yaxis.grid(b=True, which='major', color='black', linestyle='--', alpha=.4)
    ax.set_axisbelow(True)

    for tick in ax.get_xticklabels():
        tick.set_rotation(0)

#make the ticklines invisible
ax.tick_params(axis=u'both', which=u'both', length=0)
plt.tight_layout()
# remove spacing in between
fig.subplots_adjust(wspace=0)  # space between plots

plt.show()
