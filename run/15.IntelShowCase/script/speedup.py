import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import sys

#create multindex dataframe
arrays = [['higgs', 'higgs', 'higss', 'airline', 'airline', 'airline', 'yfcc','yfcc','yfcc','allstate','allstate','allstate'],
          ['8', '12', '16', '8', '12', '16', '8', '9', '10', '6', '8','9']]
index = pd.MultiIndex.from_tuples(list(zip(*arrays)))

data = np.loadtxt(sys.argv[1]).reshape((1,12))

df = pd.DataFrame(data, columns=index)

#plotting
fig, axes = plt.subplots(nrows=1, ncols=4, sharey=True, figsize=(14 / 2.54, 10 / 2.54))  # width, height
for i, col in enumerate(df.columns.levels[0]):
    print(col)
    if i >= 4:
        continue
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
