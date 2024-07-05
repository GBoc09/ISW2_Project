import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import numpy as np
import os

def load_and_plot(csv_path, metrics):
    # Load data from CSV
    df = pd.read_csv(csv_path)
    
    # Create a column for combination of Balancing, Cost_Sensitive and Classifier
    df['Combination'] = df.apply(lambda x: f"Feat_Sel={x['FEATURE_SELECTION']}, Bal={x['BALANCING']}, Cost_Sens={x['COST_SENSITIVE']}", axis=1)
    
    # Define the metrics to plot
    for metric in metrics:
        plt.figure(figsize=fig_size)
        sns.boxplot(x='CLASSIFIER', y=metric, hue='Combination', data=df)
        plt.title(f'{metric} for Different Classifiers with Various Combinations')
        plt.xticks(rotation=45)
        plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left')
        
        if y_range: 
        	plt.ylim(y_range)
        if y_ticks is not None:
        	plt.yticks(y_ticks)
        if save_path:
            if not os.path.exists(save_path):
                os.makedirs(save_path)
            plt.savefig(os.path.join(save_path, f'{metric}_boxplot.png'), bbox_inches='tight')
        
        
        
        plt.show()

# Path to the CSV file
csv_path = '/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/STORM/evaluating/merged_output_only_NpofB20.csv'

# Metrics to plot
metrics = ['PRECISION', 'RECALL', 'AUC', 'KAPPA', 'Npofb20']  # Update this list with the metrics you want to analyze

y_range = (-0.5, 1.5)
fig_size = (22,12)
y_ticks = np.arange(-0.5, 1.6, 0.1)

save_path = '/home/giulia/Immagini/Immagini'

# Load the data and generate plots
load_and_plot(csv_path, metrics)


