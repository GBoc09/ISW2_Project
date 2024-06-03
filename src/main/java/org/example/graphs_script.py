import pandas as pd
import matplotlib.pyplot as plt
import os

projects = ["BOOKKEEPER", "STORM"]
metrics = ['PRECISION', 'RECALL', 'AUC', 'KAPPA']

def get_evaluation_file(project):
	return "/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/" + project + "/evaluating/" + project +"_classifiers_report_.csv"
	

def build_output_path_plots(project):
	output_plots = "/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/" + project + "/charts/"
	print(output_plots)
	if not os.path.exists(output_plots): 
		os.makedirs(output_plots)
	return output_plots


def build_main_plots(project):
    plots_path = build_output_path_plots(project)
    main_plots_path = plots_path + "line/"
    if not os.path.exists(main_plots_path):
        os.makedirs(main_plots_path)
    return main_plots_path


def build_box_plots(project):
    plots_path = build_output_path_plots(project)
    box_plots_path = plots_path + "box/"
    if not os.path.exists(box_plots_path):
        os.makedirs(box_plots_path)
    return box_plots_path
    

def read_data(filename):
    """Reads data from a CSV file and returns a pandas DataFrame"""
    return pd.read_csv(filename)
    

def get_classifier_info(df, info):
    """Returns a list of unique classifier infos in the DataFrame"""
    metrics_mapping = {'CLASSIFIER': 0, 'FEATURE_SELECTION': 1,
                       'BALANCING': 2, 'COST_SENSITIVE': 3}
    return df['CLASSIFIER'].str.split('&').str[metrics_mapping[info]].unique().tolist()
    

def get_classifier_data(df, classifier_name):
    """Returns a DataFrame with data for a specific classifier"""
    return df[df['CLASSIFIER'].str.startswith(classifier_name)]

def filter_data(df, fs_type, s_type, cs_type):
    """Filters the DataFrame by feature selection, sampling, and cost sensitive types"""
    fs_type_str = str(fs_type)
    s_type_str = str(s_type)
    cs_type_str = str(cs_type)
    return df[df['CLASSIFIER'].str.contains(fs_type_str + '&' + s_type_str + '&' + cs_type_str)]


def get_classifier_data(df, classifier_name):
    """Returns a DataFrame with data for a specific classifier"""
    return df[df['CLASSIFIER'].str.startswith(classifier_name)]


def start_plot(fs_type, s_type, cs_type):
    """Starts a plot with title"""
    plt.figure()
    plt.title('{}, {}, {}'.format(fs_type, s_type, cs_type))



def save_plot(output_file):
    """Saves the plot to a file"""
    plt.savefig(output_file)
    plt.close()
    
    
def build_filename(output_dir, fs_type, s_type, cs_type, metric):
    output_plot_dir = output_dir + '{}_{}_{}/'.format(fs_type, s_type, cs_type)
    if not os.path.exists(output_plot_dir):
        os.makedirs(output_plot_dir)

    return output_plot_dir + '{}.png'.format(metric)


def plot_metrics(df, project_name, classifier_names, fs_type, s_type, cs_type):
    """Plots the metrics, as training releases increase, for each classifier"""

    for metric in metrics:
        df = df.dropna(subset=[metric])
        if not df.empty:
            start_plot(fs_type, s_type, cs_type)
            for i, classifier_name in enumerate(classifier_names):
                classifier_df = get_classifier_data(df, classifier_name)
                plt.plot(classifier_df['TRAINING_RELEASES'], classifier_df[metric],
                         label=classifier_name, color='C{}'.format(i))

            plt.legend()
            plt.xlabel('TRAINING_RELEASES')
            plt.ylabel(metric)

            output_file = build_filename(build_main_plots(project_name), fs_type, s_type, cs_type, metric)

            save_plot(output_file)


def plot_boxplots(df, project_name, classifier_names, fs_type, s_type, cs_type):
    """Plots a box plot of metrics for each classifier"""

    for metric in metrics:
        df = df.dropna(subset=[metric])
        if not df.empty:
            start_plot(fs_type, s_type, cs_type)
            data = []
            for classifier_name in classifier_names:
                classifier_df = get_classifier_data(df, classifier_name)
                data.append(classifier_df[metric])

            plt.boxplot(data, labels=classifier_names)
            plt.ylabel(metric)

            output_file = build_filename(build_box_plots(
                project_name), fs_type, s_type, cs_type, metric)

            save_plot(output_file)


def compute(file_name, project_name):
    # read data from CSV file
    df = read_data(file_name)

    # get unique classifier names, feature selection types, sampling types, and cost sensitive types
    classifier_names = get_classifier_info(df, 'CLASSIFIER')
    feature_selection_types = get_classifier_info(df, 'FEATURE_SELECTION')
    sampling_types = get_classifier_info(df, 'BALANCING')
    cost_sensitive_types = get_classifier_info(df, 'COST_SENSITIVE')

    # plot metrics and box plots for each combination of feature selection, sampling, and cost sensitive types
    for fs_type in feature_selection_types:
        for s_type in sampling_types:
            for cs_type in cost_sensitive_types:
                filtered_df = filter_data(df, fs_type, s_type, cs_type)

                if not filtered_df.empty:
                    plot_metrics(filtered_df, project_name, classifier_names,
                                 fs_type, s_type, cs_type)

                    plot_boxplots(filtered_df, project_name, classifier_names, fs_type, s_type, cs_type)


def main():
    for project in projects:
        file_name = get_evaluation_file(project)
        compute(file_name, project)


if __name__ == '__main__':
    main()

