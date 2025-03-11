import pandas as pd
import matplotlib.pyplot as plt

def main():
    # Read the CSV file
    df = pd.read_csv('weather_stations.csv', delimiter=';', names=['Station', 'Temperature'])

    # Generate the first plot: Station name length vs. number of stations
    df['NameLength'] = df['Station'].apply(len)
    name_length_counts = df['NameLength'].value_counts().sort_index()

    plt.figure(figsize=(10, 5))
    ax1 = name_length_counts.plot(kind='bar')
    plt.xlabel('Station Name Length')
    plt.ylabel('Number of Stations')
    plt.title('Station Name Length vs. Number of Stations')
    plt.grid(axis='y', linestyle='--')  # Dashed grid lines
    plt.xticks(rotation=45)  # Rotate x-axis labels

    # Add text labels on the bars
    for p in ax1.patches:
        ax1.annotate(str(p.get_height()), (p.get_x() * 1.005, p.get_height() * 1.005))

    plt.savefig('station_name_length.png')
    plt.show()

    # Generate the second plot: Temperature ranges vs. number of stations
    bins = [-99, -10, 0, 10, 99]
    labels = ['-99 to -10', '-10 to 0', '0 to 10', '10 to 99']
    df['TempRange'] = pd.cut(df['Temperature'], bins=bins, labels=labels, right=False)
    temp_range_counts = df['TempRange'].value_counts().sort_index()

    plt.figure(figsize=(10, 5))
    ax2 = temp_range_counts.plot(kind='bar')
    plt.xlabel('Temperature Range (°C)')
    plt.ylabel('Number of Stations')
    plt.title('Temperature Range vs. Number of Stations')
    plt.grid(axis='y', linestyle='--')  # Dashed grid lines
    plt.xticks(rotation=45)  # Rotate x-axis labels

    # Add text labels on the bars
    for p in ax2.patches:
        ax2.annotate(str(p.get_height()), (p.get_x() * 1.005, p.get_height() * 1.005))

    plt.savefig('temperature_range.png')
    plt.show()

if __name__ == "__main__":
    main()