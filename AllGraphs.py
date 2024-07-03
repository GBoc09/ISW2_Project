import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os


# Load the CSV file
file_path = '/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/BOOKKEEPER/ACUME/EAM_NEAM_output.csv'
df = pd.read_csv(file_path)

# Print the column names and first few rows to inspect the data
print("Column names:", df.columns)
print(df.head())

# Create output directory if it doesn't exist
output_dir = './plots'
os.makedirs(output_dir, exist_ok=True)

# Filter data function
def filter_data(df, sensitive, balance):
    return df[(df['COST_SENSITIVE'] == sensitive) & (df['BALANCING'] == balance)].dropna(subset=['PRECISION', 'RECALL', 'AUC', 'KAPPA'])

# Function to create combined box plot
def create_combined_box_plot(df, classifiers, filters, samplers, sensitives, metrics, title_suffix, output_dir):
    fig, axes = plt.subplots(1, len(filters) * len(samplers) * len(sensitives), figsize=(30, 10), sharey=True)
    fig.suptitle(f'Combined Box Plot of Classifier Metrics {title_suffix}', fontsize=16)

    index = 0
    for filter_val in filters:
        for sampler in samplers:
            for sensitive in sensitives:
                filtered_df = filter_data(df, sensitive, sampler)
                if not filtered_df.empty:
                    sns.boxplot(x='CLASSIFIER', y=metrics, data=filtered_df[filtered_df['FEATURE_SELECTION'] == filter_val], ax=axes[index])
                    axes[index].set_title(f'Filter: {filter_val}\nSampler: {sampler}\nSensitive: {sensitive}')
                    axes[index].set_xticklabels(classifiers, rotation=45, ha='right')
                index += 1

    plt.tight_layout(rect=[0, 0, 1, 0.95])
    plt.savefig(os.path.join(output_dir, f'combined_box_plot_{title_suffix}.png'))
    plt.close()

# Main function to create plots for each metric
def plot_metrics_for_csv(df, output_dir):
    classifiers = df['CLASSIFIER'].unique()
    filters = df['FEATURE_SELECTION'].unique()
    samplers = df['BALANCING'].unique()
    sensitives = df['COST_SENSITIVE'].unique()
    metrics_list = ['PRECISION', 'RECALL', 'AUC', 'KAPPA']

    for metric in metrics_list:
        create_combined_box_plot(df, classifiers, filters, samplers, sensitives, metric, metric, output_dir)

# Generate plots for the CSV
plot_metrics_for_csv(df, output_dir)
